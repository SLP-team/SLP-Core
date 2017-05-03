package slp.core.counting.giga;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.counting.Counter;
import slp.core.counting.trie.TrieCounter;

public class GigaCounter implements Counter {

	private static final long serialVersionUID = 8266734684040886875L;
	
	private static final int FILES_PER_COUNTER = 100;
	private static final int TOKENS_PER_COUNTER = 1000*FILES_PER_COUNTER;

	private final List<TrivialCounter> counters;
	private List<byte[]> graveyard;
	
	private final int procs;
	private final ForkJoinPool fjp;
	private int pointer;
	private int[][] counts;
	private volatile boolean[] occupied;

	private VirtualCounter counter;

	public GigaCounter() {
		this(Runtime.getRuntime().availableProcessors()/2);
	}

	public GigaCounter(int procs) {
		this.procs = procs;
		this.fjp = new ForkJoinPool(this.procs);
		
		this.counters = IntStream.range(0, this.procs).mapToObj(i -> new TrivialCounter()).collect(Collectors.toList());
		this.occupied = new boolean[this.counters.size()];
		this.counts = new int[this.counters.size()][2];
		this.pointer = 0;
		this.graveyard = new ArrayList<>();
	}

	@Override
	public int getCount() {
		resolve();
		return this.counter.getCount();
	}

	@Override
	public int[] getCounts(List<Integer> indices) {
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
			int ptr = getAvailable();
			this.fjp.submit(() -> {
				testGraveYard(ptr);
				indices.forEach(this.counters.get(ptr)::count);
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
			int ptr = getAvailable();
			this.fjp.submit(() -> {
				testGraveYard(ptr);
				this.counters.get(ptr).count(indices);
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

	private synchronized void resolve() {
		if (this.counter != null) return;
		while (IntStream.range(0, this.counters.size()).anyMatch(i -> this.occupied[i]));
		this.counters.forEach(this::pack);
		this.counters.clear();
		
		long t = System.currentTimeMillis();
		if (this.graveyard.size() >= 10) System.out.println("Resolving to VirtualCounter");
		this.counter = new VirtualCounter(unPack());
		if (this.graveyard.size() >= 10) System.out.println("Resolved in " + (System.currentTimeMillis() - t)/1000 + "s");
	}

	private void pack(TrivialCounter c) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeInt(c.getCount());
			out.writeInt(c.getCounts().size());
			c.getCounts().entrySet().stream()
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
			synchronized (this.graveyard) {
				this.graveyard.add(baos.toByteArray());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private List<TrieCounter> unPack() {
		List<TrieCounter> counters = IntStream.range(0, 4*this.procs).mapToObj(i -> new TrieCounter()).collect(Collectors.toList());
		int[] done = { 0 };
		IntStream.range(0, this.procs)
			.parallel()
			.forEach(i -> {
				for (int j = i; j < this.graveyard.size(); j += this.procs) {
					try {
						ByteArrayInputStream baos = new ByteArrayInputStream(this.graveyard.get(j));
						ObjectInputStream in = new ObjectInputStream(baos);
						int count = in.readInt();
						counters.get(0).setCount(counters.get(0).getCount() + count);
						int size = in.readInt();
						for (int q = 0; q < size; q++) {
							int len = in.readInt();
							List<Integer> key = new ArrayList<Integer>(len);
							for (int k = 0; k < len; k++) key.add(in.readInt());
							int freq = in.readInt();
							counters.get(key.get(0) % counters.size()).update(key, freq);
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
		return counters;
	}

	private int compareLists(List<Integer> key1, List<Integer> key2) {
		for (int i = 0; i < key1.size() && i < key2.size(); i++) {
			int compare = Integer.compare(key1.get(i), key2.get(i));
			if (compare != 0) return compare;
		}
		return Integer.compare(key1.size(), key2.size());
	}

	private int getAvailable() {
		while (this.occupied[this.pointer]) {
			this.pointer = (this.pointer + 1) % this.counters.size();
		}
		int ptr = this.pointer;
		this.occupied[ptr] = true;
		return ptr;
	}

	private void testGraveYard(int ptr) {
		if (this.counts[ptr][0] > FILES_PER_COUNTER || this.counts[ptr][1] > TOKENS_PER_COUNTER) {
			pack(this.counters.get(ptr));
			this.counters.set(ptr, new TrivialCounter());
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
		this.counter = new VirtualCounter();
		this.counter.readExternal(in);
	}
}
