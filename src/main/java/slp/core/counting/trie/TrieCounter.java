package slp.core.counting.trie;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class TrieCounter implements Counter, Externalizable {

	TrieCounterData data;
	public TrieCounter() {
		this(1);
	}
	
	public TrieCounter(int initSize) {
		this.data = new TrieCounterData(initSize);
	}
	
	/*
	 * Getters and setters
	 */
	public int[] getSuccIXs() {
		return this.data.indices;
	}

	@Override
	public int getCount() {
		return this.data.counts[0];
	}
	public int getContextCount() {
		return this.data.counts[1];
	}

	private int getCount(Object successor) {
		if (successor == null) return 0;
		else if (successor instanceof TrieCounter) return ((TrieCounter) successor).getCount();
		else return ((int[]) successor)[0];
	}
	
	public void setCount(int count) {
		this.data.counts[0] = count;
	}
	
	@Override
	public int getCountofCount(int n, int count) {
		return TrieCounterData.nCounts[n - 1][count - 1];
	}

	@Override
	public int[] getCounts(List<Integer> indices) {
		if (indices.isEmpty()) return new int[] { getCount(), getCount() };
		return getCounts(indices, 0);
	}

	private int[] getCounts(List<Integer> indices, int index) {
		Integer next = indices.get(index);
		Object succ = this.data.getSuccessor(next);
		int[] counts = new int[2];
		boolean nearLast = index == indices.size() - 1;
		if (nearLast) counts[1] = this.data.counts[1];
		if (succ != null) {
			if (succ instanceof TrieCounter) {
				TrieCounter successor = (TrieCounter) succ;
				if (!nearLast) return successor.getCounts(indices, index + 1);
				counts[0] = successor.getCount();
			}
			else {
				int[] successor = (int[]) succ;
				if (ArrayStorage.checkPartialSequence(indices, index, successor)) {
					counts[0] = successor[0];
					if (!nearLast) counts[1] = counts[0];
				}
				else if (!nearLast && successor.length >= indices.size() - index
						&& ArrayStorage.checkPartialSequence(indices.subList(0, indices.size() - 1), index, successor)) {
					counts[1] = successor[0];
				}
			}
		}
		return counts;
	}

	@Override
	public int[] getDistinctCounts(int range, List<Integer> indices) {
		return getDistinctCounts(range, indices, 0);
	}

	private int[] getDistinctCounts(int range, List<Integer> indices, int index) {
		if (index < indices.size()) {
			Integer next = indices.get(index);
			Object succ = this.data.getSuccessor(next);
			if (succ == null) return new int[range];
			if (succ instanceof TrieCounter) {
				TrieCounter successor = (TrieCounter) succ;
				return successor.getDistinctCounts(range, indices, index + 1);
			}
			else {
				int[] successor = (int[]) succ;
				int[] distinctCounts = new int[range];
				if (ArrayStorage.checkPartialSequence(indices, index, successor)
						&& !ArrayStorage.checkExactSequence(indices, index, successor)) {
					distinctCounts[Math.min(range - 1, successor[0] - 1)]++;
				}
				return distinctCounts;
			}
		} else {
			int[] distinctCounts = new int[range];
			int totalDistinct = this.getSuccessorCount();
			for (int i = 2; i < this.data.counts.length - 1 && i - 1 < range; i++) {
				int countOfCountsI = this.data.counts[i];
				distinctCounts[i - 2] = countOfCountsI;
				totalDistinct -= countOfCountsI;
			}
			distinctCounts[range - 1] = totalDistinct;
			return distinctCounts;
		}
	}

	@Override
	public int getSuccessorCount() {
		return Arrays.stream(this.data.counts).skip(2).sum();
	}

	@Override
	public int getSuccessorCount(List<Integer> indices) {
		Object successor = getSuccessorNode(indices, 0);
		if (successor == null) return 0;
		else if (successor instanceof TrieCounter) return ((TrieCounter) successor).getSuccessorCount();
		else return 1;
	}

	@Override
	public List<Integer> getTopSuccessors(List<Integer> indices, int limit) {
		return IntStream.range(0, this.data.indices.length)
			.filter(i -> this.data.indices[i] != Integer.MAX_VALUE)
			.mapToObj(i -> Pair.of(i, this.getCount(this.data.successors[i])))
			.filter(p -> p.right != null && p.right > 0)
			.sorted((p1, p2) -> -Integer.compare(p1.right, p2.right))
			.limit(limit)
			.map(p -> p.left)
			.collect(Collectors.toList());
	}

	private Object getSuccessorNode(List<Integer> indices, int index) {
		if (index == indices.size()) return this;
		Integer next = indices.get(index);
		Object succ = this.data.getSuccessor(next);
		if (succ == null) return null;
		else if (succ instanceof TrieCounter) {
			TrieCounter successor = (TrieCounter) succ;
			return successor.getSuccessorNode(indices, index + 1);
		}
		else {
			int[] successor = (int[]) succ;
			if (!ArrayStorage.checkPartialSequence(indices, index, successor)) return null;
			int[] trueSucc = new int[1 + successor.length - (indices.size() - index)];
			trueSucc[0] = successor[0];
			for (int i = 1; i < trueSucc.length; i++) {
				trueSucc[i] = successor[i + indices.size() - index - 1];
			}
			return trueSucc;
		}
	}

	@Override
	public final void count(List<Integer> indices) {
		update(indices, 1);
	}

	@Override
	public final void unCount(List<Integer> indices) {
		update(indices, -1);
	}

	public final void update(List<Integer> indices, int adj) {
		update(indices, 0, adj);
	}

	/*
	 * Trie Updating
	 */
	public synchronized void update(List<Integer> indices, int index, int adj) {
		if (index < indices.size()) {
			Integer key = indices.get(index);
			Object successor = this.data.getSuccessor(key);
			if (successor != null) this.data.updateSuccessor(indices, index, adj, successor);
			else this.data.addSucessor(indices, index, adj);
		}
		this.data.updateCount(adj, index == indices.size());
		this.data.updateNCounts(index, this.getCount(), adj);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.data.counts[0]);
		out.writeInt(this.data.counts[1]);
		out.writeInt(this.getSuccessorCount());
		for (int i = 0; i < this.data.indices.length; i++) {
			if (this.data.indices[i] == Integer.MAX_VALUE) continue;
			out.writeInt(this.data.indices[i]);
			Object o = this.data.successors[i];
			if (o instanceof int[]) {
				int[] arr = (int[]) o;
				out.writeInt(arr.length);
				for (int j = 0; j < arr.length; j++) out.writeInt(arr[j]);
			}
			else {
				out.writeInt(-1);
				out.writeObject((TrieCounter) o);
			}
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.data.counts = new int[2 + TrieCounterData.COUNT_OF_COUNTS_CUTOFF];
		this.data.counts[0] = in.readInt();
		this.data.counts[1] = in.readInt();
		int successors = in.readInt();
		this.data.indices = new int[successors];
		this.data.successors = new Object[successors];
		int pos = 0;
		for (; pos < successors; pos++) {
			int key = in.readInt();
			int code = in.readInt();
			Object value;
			if (code < 0) {
				value = in.readObject();
				this.data.counts[1 + Math.min(((TrieCounter) value).getCount(), TrieCounterData.COUNT_OF_COUNTS_CUTOFF)]++;
			}
			else {
				value = new int[code];
				for (int j = 0; j < code; j++) ((int[]) value)[j] = in.readInt();
				this.data.counts[1 + Math.min(((int[]) value)[0], TrieCounterData.COUNT_OF_COUNTS_CUTOFF)]++;
			}
			this.data.indices[pos] = key;
			this.data.successors[pos] = value;
		}
	}
}
