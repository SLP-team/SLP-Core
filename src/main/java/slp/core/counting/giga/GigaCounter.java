package slp.core.counting.giga;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.counting.Counter;
import slp.core.counting.trie.TrieCounter;

/**
 * Class for counting very large corpora.
 * Helpful especially when needing to train once on a very large corpus with only none or 'normal-sized' updates afterwards
 * (though mixing is quite fine).<br /><br />
 * 
 * Very large corpora cause slow-downs in both training and testing for the conventional {@link TrieCounter}
 * due to garbage collection and binary-search lookup.
 * The {@link GigaCounter} solves this in three ways:
 * <ul>
 * <li>It counts in parallel
 * <li>It serializes batches of counted files into a single byte array at train-time (dramatically reducing gc overhead)
 * <li>Finally, when done training it resolves the serialized counters in parallel into a {@link VirtualCounter} and defers all future calls to that object.
 * </ul>
 * The {@link VirtualCounter} in turn also has mechanisms to deal better with parallel updating and lookup than the {@link TrieCounter}.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class GigaCounter implements Counter {

	private static final long serialVersionUID = 8266734684040886875L;
	
	private static final int FILES_PER_COUNTER = 100;
	private static final int TOKENS_PER_COUNTER = 1000*FILES_PER_COUNTER;

	private final List<Map<List<Integer>, Integer>> simpleCounters;
	private List<byte[]> graveyard;
	
	private final int procs;
	private final ForkJoinPool fjp;
	private int[][] counts;
	private volatile boolean[] occupied;

	private VirtualCounter counter;

	public GigaCounter() {
		this(Runtime.getRuntime().availableProcessors()/2);
	}

	public GigaCounter(int procs) {
		this.procs = procs;
		this.fjp = new ForkJoinPool(this.procs);
		
		this.simpleCounters = IntStream.range(0, this.procs).mapToObj(i -> new HashMap<List<Integer>, Integer>()).collect(Collectors.toList());
		this.occupied = new boolean[this.simpleCounters.size()];
		this.counts = new int[this.simpleCounters.size()][2];
		this.graveyard = Collections.synchronizedList(new ArrayList<>());
	}

	@Override
	public int getCount() {
		resolve();
		return this.counter.getCount();
	}

	@Override
	public long[] getCounts(List<Integer> indices) {
		resolve();
		return this.counter.getCounts(indices);
	}

	@Override
	public int getCountofCount(int n, int count) {
		resolve();
		return this.counter.getCountofCount(n, count);
	}

	@Override
	public int getSuccessorCount() {
		resolve();
		return this.counter.getSuccessorCount();
	}

	@Override
	public int getSuccessorCount(List<Integer> indices) {
		resolve();
		return this.counter.getSuccessorCount(indices);
	}

	@Override
	public List<Integer> getTopSuccessors(List<Integer> indices, int limit) {
		resolve();
		return this.counter.getTopSuccessors(indices, limit);
	}

	@Override
	public int[] getDistinctCounts(int range, List<Integer> indices) {
		resolve();
		return this.counter.getDistinctCounts(range, indices);
	}

	@Override
	public void countBatch(List<List<Integer>> indices) {
		if (this.counter != null) {
			this.counter.countBatch(indices);
		}
		else {
			int ptr = getNextAvailable();
			this.occupied[ptr] = true;
			this.fjp.submit(() -> {
				testGraveYard(ptr);
				indices.forEach(ix -> this.simpleCounters.get(ptr).merge(ix, 1, Integer::sum));
				this.counts[ptr][0]++;
				this.occupied[ptr] = false;
			});
		}
	}
	
	@Override
	public void count(List<Integer> indices) {
		if (this.counter != null) {
			this.counter.count(indices);
		}
		else {
			int ptr = getNextAvailable();
			this.occupied[ptr] = true;
			this.fjp.submit(() -> {
				testGraveYard(ptr);
				this.simpleCounters.get(ptr).merge(indices, 1, Integer::sum);
				this.counts[ptr][1]++;
				this.occupied[ptr] = false;
			});
		}
	}

	@Override
	public void unCount(List<Integer> indices) {
		resolve();
		this.counter.unCount(indices);
	}

	private void resolve() {
		if (this.counter != null) return;
		while (IntStream.range(0, this.simpleCounters.size()).anyMatch(i -> this.occupied[i]));
		
		if (this.graveyard.size() >= 10) System.out.println("Resolving to VirtualCounter");
		long t = System.currentTimeMillis();
		this.simpleCounters.stream().filter(c -> !c.isEmpty()).forEach(this::pack);
		this.simpleCounters.clear();
		this.counter = new VirtualCounter(16);
		unPackAll();
		if (this.graveyard.size() >= 10) System.out.println("Resolved in " + (System.currentTimeMillis() - t)/1000 + "s");
		System.gc();
	}

	private void pack(Map<List<Integer>, Integer> c) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeInt(c.size());
			c.entrySet().stream()
				.sorted((e1, e2) -> compareLists(e1.getKey(), e2.getKey()))
				.forEach(e -> {
					List<Integer> key = e.getKey();
					Integer value = e.getValue();
					try {
						out.writeInt(key.size());
						for (int k : key) out.writeInt(k);
						out.writeInt(value);
					} catch (IOException ex) {
					}
				});
			out.close();
			this.graveyard.add(baos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void unPackAll() {
		int[] done = { 0 };
		IntStream.range(0, this.procs)
			.parallel()
			.forEach(i -> {
				for (int j = i; j < this.graveyard.size(); j += this.procs) {
					try {
						ByteArrayInputStream baos = new ByteArrayInputStream(this.graveyard.get(j));
						ObjectInputStream in = new ObjectInputStream(baos);
						int size = in.readInt();
						for (int q = 0; q < size; q++) {
							int len = in.readInt();
							List<Integer> key = new ArrayList<Integer>(len);
							for (int k = 0; k < len; k++) key.add(in.readInt());
							int freq = in.readInt();
							this.counter.count(key, freq);
						}
						in.close();
						this.graveyard.set(j, null);
						if (this.graveyard.size() >= 10 && ++done[0] % (this.graveyard.size() / 10) == 0) {
							System.out.print(100*done[0] / this.graveyard.size() + "%...");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		if (this.graveyard.size() >= 10) System.out.println();
	}

	private int compareLists(List<Integer> key1, List<Integer> key2) {
		for (int i = 0; i < key1.size() && i < key2.size(); i++) {
			int compare = Integer.compare(key1.get(i), key2.get(i));
			if (compare != 0) return compare;
		}
		return Integer.compare(key1.size(), key2.size());
	}

	private int getNextAvailable() {
		int ptr = 0;
		while (this.occupied[ptr]) {
			ptr = (ptr + 1) % this.simpleCounters.size();
		}
		return ptr;
	}

	private void testGraveYard(int ptr) {
		if (this.counts[ptr][0] > FILES_PER_COUNTER || this.counts[ptr][1] > TOKENS_PER_COUNTER) {
			pack(this.simpleCounters.get(ptr));
			this.simpleCounters.set(ptr, new HashMap<>());
			this.counts[ptr][0] = 0;
			this.counts[ptr][1] = 0;
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		resolve();
		this.counter.writeExternal(out);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.counter = new VirtualCounter(0);
		this.counter.readExternal(in);
	}
}
