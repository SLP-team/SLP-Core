package slp.core.counting.giga;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.counting.Counter;
import slp.core.counting.trie.TrieCounter;

public class VirtualCounter implements Counter {

	private static final long serialVersionUID = 8266734684040886875L;

	private List<TrieCounter> counters;

	public VirtualCounter() {
		this(1);
	}
	
	public VirtualCounter(int counters) {
		this(IntStream.range(0, counters).mapToObj(i -> new TrieCounter()).collect(Collectors.toList()));
	}
	
	public VirtualCounter(List<TrieCounter> counters) {
		this.counters = counters;
	}

	@Override
	public int getCount() {
		return this.counters.get(0).getCount();
	}

	private int mem = 0;
	@Override
	public int[] getCounts(List<Integer> indices) {
		int[] counts = this.counters.get(indices.isEmpty() ? 0 : indices.get(0) % this.counters.size()).getCounts(indices);
		if (indices.size() == 1) {
			if (this.mem == 0) {
				this.mem = IntStream.range(0, this.counters.size()).map(i -> this.counters.get(i).getContextCount()).sum();
			}
			counts[1] = this.mem;
		}
		return counts;
	}

	@Override
	public int getCountofCount(int n, int count) {
		return this.counters.stream().parallel().mapToInt(c -> getCountofCount(n, count)).sum();
	}

	@Override
	public int getSuccessorCount() {
		return this.counters.stream().parallel().mapToInt(Counter::getSuccessorCount).sum();
	}

	@Override
	public int getSuccessorCount(List<Integer> indices) {
		return this.counters.get(indices.isEmpty() ? 0 : indices.get(0) % this.counters.size()).getSuccessorCount(indices);
	}

	@Override
	public List<Integer> getTopSuccessors(List<Integer> indices, int limit) {
		return this.counters.stream().parallel()
				.flatMap(c -> c.getTopSuccessors(indices, limit).stream()).collect(Collectors.toList());
	}

	@Override
	public int[] getDistinctCounts(int range, List<Integer> indices) {
		return this.counters.get(indices.isEmpty() ? 0 : indices.get(0) % this.counters.size()).getDistinctCounts(range, indices);
	}

	@Override
	public void count(List<Integer> indices) {
		this.mem = 0;
		this.counters.get(indices.isEmpty() ? 0 : indices.get(0) % this.counters.size()).count(indices);
	}

	@Override
	public void unCount(List<Integer> indices) {
		this.mem = 0;
		this.counters.get(indices.isEmpty() ? 0 : indices.get(0) % this.counters.size()).unCount(indices);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.counters.size());
		for (Counter counter : this.counters) out.writeObject(counter);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int size = in.readInt();
		for (int i = 0; i < size; i++) {
			this.counters.add((TrieCounter) in.readObject());
		}
	}
}
