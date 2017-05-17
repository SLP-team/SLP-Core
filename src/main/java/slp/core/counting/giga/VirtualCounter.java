package slp.core.counting.giga;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.counting.Counter;
import slp.core.counting.trie.MapTrieCounter;

/**
 * Wraps multiple counters (specifically {@link MapTrieCounter}s) to support faster concurrent access 
 * and reduce garbage collection cost per counter.<br />
 * Each counter c_i is assigned all (integer) sequences s=[t0, t1, ...] such that (t0 % counters.size()) == i.
 * The empty sequence is assigned to c_0.
 * <br /><br />
 * 
 * The {@link GigaCounter} is the primary user of this VirtualCounter
 * since it must both deal with high memory usage (and thus GC) and since it 'unpacks' its internal counters in parallel. <br />
 * The choice of {@link MapTrieCounter} internally further boosts counting throughput (and reduces gc) at the expense of (slight) memory increase,
 * since that Trie uses hashmaps instead of binary-sorted arrays internally.<br /><br />
 * 
 * API note: as stated, the current implementation uses a hard-coded Counter type, which is known to synchronize its update method.
 * Future versions may explicitly synchronize counting themselves and allow generic counters to be stored.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class VirtualCounter implements Counter {

	private static final long serialVersionUID = 8266734684040886875L;

	private List<MapTrieCounter> counters;

	public VirtualCounter() {
		this(1);
	}
	
	public VirtualCounter(int counters) {
		this(IntStream.range(0, counters).mapToObj(i -> new MapTrieCounter()).collect(Collectors.toList()));
	}
	
	public VirtualCounter(List<MapTrieCounter> counters) {
		this.counters = counters;
	}

	@Override
	public int getCount() {
		return this.counters.get(0).getCount();
	}

	private long memCC = 0;
	@Override
	public long[] getCounts(List<Integer> indices) {
		long[] counts = this.counters.get(getIndex(indices)).getCounts(indices);
		if (indices.size() == 1) {
			if (this.memCC == 0) {
				this.memCC = IntStream.range(0, this.counters.size()).map(i -> this.counters.get(i).getContextCount()).sum();
			}
			counts[1] = this.memCC;
		}
		return counts;
	}

	@Override
	public int getCountofCount(int n, int count) {
		// Count of counts table is static and shared between MapTrieCounters, so this relies strongly on using that class!
		return this.counters.get(0).getCountofCount(n, count);
	}

	private int memSC = 0;
	@Override
	public int getSuccessorCount() {
		if (this.memSC == 0) this.memSC = this.counters.stream().mapToInt(Counter::getSuccessorCount).sum();
		return this.memSC;
	}

	@Override
	public int getSuccessorCount(List<Integer> indices) {
		return this.counters.get(getIndex(indices)).getSuccessorCount(indices);
	}

	@Override
	public List<Integer> getTopSuccessors(List<Integer> indices, int limit) {
		if (!indices.isEmpty()) return this.counters.get(getIndex(indices)).getTopSuccessors(indices, limit);
		else {
			// TODO
			return null;
		}
	}
	
	private int[] memDS = null;
	@Override
	public int[] getDistinctCounts(int range, List<Integer> indices) {
		if (indices.isEmpty()) {
			if (this.memDS == null || this.memDS.length != range) {
				this.memDS = new int[range];
				this.counters.stream().map(c -> c.getDistinctCounts(range, indices))
					.forEach(d -> IntStream.range(0, d.length).forEach(j -> this.memDS[j] += d[j]));
			}
			return this.memDS;
		}
		else return this.counters.get(getIndex(indices)).getDistinctCounts(range, indices);
	}

	@Override
	public void count(List<Integer> indices) {
		this.memCC = 0;
		this.memSC = 0;
		this.memDS = null;
		this.counters.get(getIndex(indices)).count(indices);
	}

	public void count(List<Integer> indices, int frequency) {
		this.memCC = 0;
		this.memSC = 0;
		this.memDS = null;
		this.counters.get(getIndex(indices)).update(indices, frequency);
	}

	@Override
	public void unCount(List<Integer> indices) {
		this.memCC = 0;
		this.memSC = 0;
		this.memDS = null;
		this.counters.get(getIndex(indices)).unCount(indices);
	}

	private int getIndex(List<Integer> key) {
		return key.isEmpty() ? 0 : key.get(0) % this.counters.size();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.counters.size());
		for (MapTrieCounter counter : this.counters) counter.writeExternal(out);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.counters.clear();
		int size = in.readInt();
		for (int i = 0; i < size; i++) {
			MapTrieCounter counter = new MapTrieCounter(0);
			counter.readExternal(in);
			this.counters.add(counter);
		}
	}
}
