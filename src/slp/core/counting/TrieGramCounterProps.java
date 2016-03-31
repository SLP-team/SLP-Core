package slp.core.counting;

import com.google.common.collect.TreeMultimap;

public class TrieGramCounterProps implements CounterProps<TrieGramCounter> {

	private TrieGramCounter counter;
	
	public TrieGramCounterProps(TrieGramCounter trieGramCounter) {
		this.counter = trieGramCounter;
	}
	
	@Override
	public TrieGramCounter getCounter() {
		return this.counter;
	}

	@Override
	public int getOverallCount() {
		return this.counter.getCount();
	}
	
	@Override
	public int getOverallDistinct() {
		return this.counter.getCounts().size();
	}

	@Override
	public int[][] getFullCounts(Integer[] indices) {
		return getFullCounts(indices, 0);
	}

	@Override
	public int[][] getFullCounts(Integer[] indices, int startIndex) {
		return getFullCounts(indices, startIndex, indices.length);
	}

	@Override
	public int[][] getFullCounts(Integer[] indices, int startIndex, int endIndex) {
		int[][] counts = new int[2][endIndex - startIndex];
		if (startIndex > endIndex) {
			System.err.println("Invalid start/end index " + startIndex + "/" + endIndex);
			return counts;
		}
		for (int i = startIndex; i < endIndex; i++) {
			int offset = endIndex - i - 1;
			getFullCounts(indices, this.counter, counts, i, endIndex, offset);
		}
		return counts;
	}

	private void getFullCounts(Integer[] indices, TrieGramCounter counter, int[][] counts, int index, int end, int offset) {
		Integer next = indices[index];
		TrieGramCounter successor = counter.getSuccessor(next);
		if (index == end - 1) {
			counts[1][offset] = counter.getCount();
			counts[0][offset] = successor == null ? 0 : successor.getCount();
		}
		else if (successor != null) {
			getFullCounts(indices, successor, counts, index + 1, end, offset);
		}
	}

	@Override
	public int[] getShortCounts(Integer[] indices) {
		return getShortCounts(indices, 0);
	}

	@Override
	public int[] getShortCounts(Integer[] indices, int startIndex) {
		return getShortCounts(indices, startIndex, indices.length);
	}

	@Override
	public int[] getShortCounts(Integer[] indices, int startIndex, int endIndex) {
		int[] counts = new int[2];
		if (startIndex > endIndex) {
			System.err.println("Invalid start/end index " + startIndex + "/" + endIndex);
			return counts;
		}
		TrieGramCounter[] counters = findCounterPath(indices, startIndex, endIndex);
		counts[0] = counters[counters.length - 1].getCount();
		counts[1] = counters[counters.length - 2].getCount();
		return counts;
	}

	@Override
	public int[] getDistinctCounts(int range, Integer[] indices) {
		return getDistinctCounts(range, indices, 0);
	}

	@Override
	public int[] getDistinctCounts(int range, Integer[] indices, int startIndex) {
		return getDistinctCounts(range, indices, startIndex, indices.length - 1);
	}

	@Override
	/**
	 * @return the number of distinct successors with counts 1 through {@code range} 
	 * that have followed the provided sequence.
	 */
	public int[] getDistinctCounts(int range, Integer[] indices, int startIndex, int endIndex) {
		int[] distinctCounts = new int[range];
		TrieGramCounter counter = findCounterPath(this.counter, indices, startIndex, endIndex);
		TreeMultimap<Integer, TrieGramCounter> counts = counter.getCounts();
		int overallCount = counts.size();
		for (int i = 1; i < range; i++) {
			int countI = counts.get(i).size();
			distinctCounts[i] = countI;
			overallCount -= countI;
		}
		distinctCounts[range - 1] = overallCount;
		return distinctCounts;
	}

	private TrieGramCounter findCounterPath(TrieGramCounter counter, Integer[] indices, int startIndex, int endIndex) {
		if (startIndex == endIndex - 1) {
			return counter;
		}
		else {
			Integer next = indices[startIndex];
			TrieGramCounter successor = counter.getSuccessor(next);
			if (successor == null) return null;
			else return findCounterPath(successor, indices, startIndex + 1, endIndex);
		}
	}

	private TrieGramCounter[] findCounterPath(Integer[] indices, int startIndex, int endIndex) {
		TrieGramCounter[] counters = new TrieGramCounter[endIndex - startIndex + 1];
		counters[0] = this.counter;
		findCounterPath(counters, indices, 1, startIndex, endIndex);
		return counters;
	}
	
	private void findCounterPath(TrieGramCounter[] counters, Integer[] indices, int offset, int startIndex, int endIndex) {
		if (startIndex < endIndex) {
			Integer next = indices[startIndex];
			TrieGramCounter successor = counters[startIndex].getSuccessor(next);
			if (successor != null) {
				counters[offset] = successor;
				findCounterPath(counters, indices, offset + 1, startIndex + 1, endIndex);
			}
		}
	}
}
