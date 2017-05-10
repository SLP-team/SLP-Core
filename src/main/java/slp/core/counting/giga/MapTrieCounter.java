package slp.core.counting.giga;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import gnu.trove.map.hash.TIntObjectHashMap;
import slp.core.counting.Counter;
import slp.core.counting.trie.ArrayStorage;
import slp.core.counting.trie.TrieCounterData;
import slp.core.util.Pair;

public class MapTrieCounter implements Counter, Externalizable {

	/**
	 * 'counts' contains in order: own count, context count (sum of successor's counts),
	 * no of distinct successors seen once, twice, up to the COCcutoff in Configuration
	 */
	private int[] counts;
	private TIntObjectHashMap<Object> map;
	
	public MapTrieCounter() {
		this(1);
	}

	public MapTrieCounter(int initSize) {
		this.counts = new int[2 + TrieCounterData.COUNT_OF_COUNTS_CUTOFF];
		this.map = new TIntObjectHashMap<>(initSize);
	}

	/*
	 * Getters and setters
	 */
	@Override
	public int getCount() {
		return this.counts[0];
	}
	
	public int getContextCount() {
		return this.counts[1];
	}

	private int getCount(Object successor) {
		if (successor == null) return 0;
		else if (successor instanceof MapTrieCounter) return ((MapTrieCounter) successor).getCount();
		else return ((int[]) successor)[0];
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
		Object succ = this.map.get(next);
		int[] counts = new int[2];
		boolean nearLast = index == indices.size() - 1;
		if (nearLast) counts[1] = this.counts[1];
		if (succ != null) {
			if (succ instanceof MapTrieCounter) {
				MapTrieCounter successor = (MapTrieCounter) succ;
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
			Object succ = this.map.get(next);
			if (succ == null) return new int[range];
			if (succ instanceof MapTrieCounter) {
				MapTrieCounter successor = (MapTrieCounter) succ;
				return successor.getDistinctCounts(range, indices, index + 1);
			}
			else {
				int[] successor = (int[]) succ;
				int[] distinctCounts = new int[range];
				if (ArrayStorage.checkPartialSequence(indices, index, successor)
						&& !ArrayStorage.checkExactSequence(indices, index, successor)) {
					distinctCounts[Math.min(range - 1, successor[0] - 1)] = 1;
				}
				return distinctCounts;
			}
		} else {
			int[] distinctCounts = new int[range];
			int totalDistinct = this.getSuccessorCount();
			for (int i = 2; i < this.counts.length - 1 && i - 1 < range; i++) {
				int countOfCountsI = this.counts[i];
				distinctCounts[i - 2] = countOfCountsI;
				totalDistinct -= countOfCountsI;
			}
			distinctCounts[range - 1] = totalDistinct;
			return distinctCounts;
		}
	}

	@Override
	public int getSuccessorCount() {
		return Arrays.stream(this.counts).skip(2).sum();
	}

	@Override
	public int getSuccessorCount(List<Integer> indices) {
		Object successor = getSuccessorNode(indices, 0);
		if (successor == null) return 0;
		else if (successor instanceof MapTrieCounter) return ((MapTrieCounter) successor).getSuccessorCount();
		else return 1;
	}

	@Override
	public List<Integer> getTopSuccessors(List<Integer> indices, int limit) {
		int[] keys = this.map.keys();
		Object[] values = this.map.values();
		return IntStream.range(0, keys.length)
			.mapToObj(i -> Pair.of(keys[i], this.getCount(values[i])))
			.filter(p -> p.right != null && p.right > 0)
			.sorted((p1, p2) -> -Integer.compare(p1.right, p2.right))
			.limit(limit)
			.map(p -> p.left)
			.collect(Collectors.toList());
	}

	private Object getSuccessorNode(List<Integer> indices, int index) {
		if (index == indices.size()) return this;
		Integer next = indices.get(index);
		Object succ = this.map.get(next);
		if (succ == null) return null;
		else if (succ instanceof MapTrieCounter) {
			MapTrieCounter successor = (MapTrieCounter) succ;
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

	/*
	 * Trie Updating
	 */
	@Override
	public final void count(List<Integer> indices) {
		update(indices, 1);
	}

	@Override
	public final void unCount(List<Integer> indices) {
		update(indices, -1);
	}

	public void updateCount(int adj) {
		update(Collections.emptyList(), adj);
	}

	public final void update(List<Integer> indices, int adj) {
		update(indices, 0, adj);
	}
	
	public synchronized void update(List<Integer> indices, int index, int adj) {
		if (index < indices.size()) {
			Integer key = indices.get(index);
			Object successor = this.map.get(key);
			if (successor != null) this.updateSuccessor(indices, index, adj, successor);
			else this.addSucessor(indices, index, adj);
		}
		this.counts[0] += adj;
		if (index != indices.size()) this.counts[1] += adj;
		TrieCounterData.updateNCounts(index, this.getCount(), adj);
	}

	private void updateSuccessor(List<Integer> indices, int index, int adj, Object succ) {
		if (succ instanceof MapTrieCounter) updateTrie(indices, index, adj, succ);
		else updateArray(indices, index, adj, succ);
	}

	private void updateTrie(List<Integer> indices, int index, int adj, Object succ) {
		MapTrieCounter next = (MapTrieCounter) succ;
		next.update(indices, index + 1, adj);
		updateCoCs(next.getCount(), adj);
		if (next.getCount() == 0) {
			this.map.remove(indices.get(index));
		}
	}

	private void updateArray(List<Integer> indices, int index, int adj, Object succ) {
		int[] successor = (int[]) succ;
		boolean valid = ArrayStorage.checkExactSequence(indices, index, successor);
		if (valid) updateArrayCount(indices, index, adj, successor);
		else {
			if (adj < 0) System.err.println("Attempting to unsee never seen event");
			MapTrieCounter newNext = promoteArrayToTrie(indices, index, successor);
			updateTrie(indices, index, adj, newNext);
		}
	}

	private void updateArrayCount(List<Integer> indices, int index, int adj, int[] successor) {
		successor[0] += adj;
		if (successor[0] == 0) {
			this.map.remove(indices.get(index));
		}
		updateCoCs(successor[0], adj);
		for (int i = index + 1; i <= indices.size(); i++) {
			TrieCounterData.updateNCounts(i, successor[0], adj);
		}
	}

	private MapTrieCounter promoteArrayToTrie(List<Integer> indices, int index, int[] successor) {
		MapTrieCounter newNext = new MapTrieCounter(1);
		newNext.updateCount(successor[0]);
		if (successor.length > 1) {
			newNext.counts[1] = newNext.counts[0];
			int[] temp = Arrays.copyOfRange(successor, 1, successor.length);
			temp[0] = successor[0];
			newNext.map.put(successor[1], temp);
			if (TrieCounterData.COUNT_OF_COUNTS_CUTOFF > 0) {
				newNext.counts[1 + Math.min(temp[0], TrieCounterData.COUNT_OF_COUNTS_CUTOFF)]++;
			}
		}
		this.map.put(indices.get(index), newNext);
		return newNext;
	}

	private void addSucessor(List<Integer> indices, int index, int adj) {
		if (adj < 0) {
			System.out.println("Attempting to store new event with negative count: " + indices.subList(index, indices.size()));
			return;
		}
		int[] singleton = new int[indices.size() - index];
		singleton[0] = adj;
		for (int i = 1; i < singleton.length; i++) {
			singleton[i] = indices.get(index + i);
		}
		this.map.put(indices.get(index), singleton);
		updateCoCs(adj, adj);
		for (int i = index + 1; i <= indices.size(); i++) {
			TrieCounterData.updateNCounts(i, adj, adj);
		}
	}

	private void updateCoCs(int count, int adj) {
		if (TrieCounterData.COUNT_OF_COUNTS_CUTOFF == 0) return;
		int currIndex = Math.min(count, TrieCounterData.COUNT_OF_COUNTS_CUTOFF);
		int prevIndex = Math.min(count - adj, TrieCounterData.COUNT_OF_COUNTS_CUTOFF);
		if (currIndex != prevIndex) {
			if (currIndex >= 1) this.counts[currIndex + 1]++;
			if (prevIndex >= 1) this.counts[prevIndex + 1]--;
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.counts[0]);
		out.writeInt(this.counts[1]);
		out.writeInt(this.map.size());
		int[] keys = this.map.keys();
		Object[] values = this.map.values();
		for (int i = 0; i < keys.length; i++) {
			out.writeInt(keys[i]);
			Object o = values[i];
			if (o instanceof int[]) {
				int[] arr = (int[]) o;
				out.writeInt(arr.length);
				for (int j = 0; j < arr.length; j++) out.writeInt(arr[j]);
			}
			else {
				out.writeInt(-1);
				out.writeObject((MapTrieCounter) o);
			}
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.counts = new int[2 + TrieCounterData.COUNT_OF_COUNTS_CUTOFF];
		this.counts[0] = in.readInt();
		this.counts[1] = in.readInt();
		int successors = in.readInt();
		this.map = new TIntObjectHashMap<>(successors);
		int pos = 0;
		for (; pos < successors; pos++) {
			int key = in.readInt();
			int code = in.readInt();
			Object value;
			if (code < 0) {
				value = in.readObject();
				this.counts[1 + Math.min(((MapTrieCounter) value).getCount(), TrieCounterData.COUNT_OF_COUNTS_CUTOFF)]++;
			}
			else {
				value = new int[code];
				for (int j = 0; j < code; j++) ((int[]) value)[j] = in.readInt();
				this.counts[1 + Math.min(((int[]) value)[0], TrieCounterData.COUNT_OF_COUNTS_CUTOFF)]++;
			}
			this.map.put(key, value);
		}
	}
}
