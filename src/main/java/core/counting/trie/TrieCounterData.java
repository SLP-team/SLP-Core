package core.counting.trie;

import java.util.Arrays;
import java.util.List;

import core.modeling.ModelRunner;

public class TrieCounterData {
	/**
	 * 'counts' contains in order: own count, context count (sum of successor's counts),
	 * no of distinct successors seen once, twice, up to the COCcutoff in Configuration
	 */
	public int[] counts;
	public int[] indices;
	public Object[] successors;
	
	public static int COUNT_OF_COUNTS_CUTOFF = 3;
	static int[][] nCounts = new int[ModelRunner.getNGramOrder()][4];
	private static final double GROWTH_FACTOR = 1.1;

	public TrieCounterData(int initSize) {
		this.counts = new int[2 + COUNT_OF_COUNTS_CUTOFF];
		this.indices = new int[initSize];
		this.successors = new Object[initSize];
		Arrays.fill(this.indices, Integer.MAX_VALUE);
	}

	void updateSuccessor(List<Integer> indices, int index, boolean count, Integer key, Object succ) {
		if (succ instanceof TrieCounter) updateTrie(indices, index, count, key, succ);
		else updateArray(indices, index, count, key, succ);
	}

	private void updateTrie(List<Integer> indices, int index, boolean count, Integer key, Object succ) {
		TrieCounter next = (TrieCounter) succ;
		next.update(indices, index + 1, count);
		updateCoCs(next.getCount(), count);
		if (next.getCount() == 0) {
			removeSucc(key);
		}
	}

	private void updateArray(List<Integer> indices, int index, boolean count, Integer key, Object succ) {
		int[] successor = (int[]) succ;
		boolean valid = ArrayStorage.checkExactSequence(indices, index, successor);
		if (valid) updateArrayCount(indices, index, count, key, successor);
		else promoteArrayToTrie(indices, index, count, key, successor);
	}

	private void updateArrayCount(List<Integer> indices, int index, boolean count, Integer key, int[] successor) {
		successor[0] += (count ? 1 : -1);
		updateCoCs(successor[0], count);
		if (successor[0] == 0) {
			removeSucc(key);
		}
		for (int i = index + 1; i <= indices.size(); i++) {
			this.updateNCounts(i, successor[0], count);
		}
	}

	private void promoteArrayToTrie(List<Integer> indices, int index, boolean count, Integer key, int[] successor) {
		if (!count) System.err.println("Attempting to unsee never seen event");
		TrieCounter newNext = new TrieCounter(1);
		newNext.setCount(successor[0]);
		if (successor.length > 1) {
			newNext.data.counts[1] = newNext.data.counts[0];
			int[] temp = Arrays.copyOfRange(successor, 1, successor.length);
			temp[0] = successor[0];
			newNext.data.putSucc(successor[1], temp);
			if (COUNT_OF_COUNTS_CUTOFF > 0) newNext.data.counts[1 + Math.min(temp[0], COUNT_OF_COUNTS_CUTOFF)]++;
		}
		putSucc(key, newNext);
		updateTrie(indices, index, count, key, newNext);
	}

	void addSucessor(List<Integer> indices, int index, boolean count, Integer key) {
		if (!count) {
			System.out.println("Attempting to unsee never seen event: " + key + "\t" + indices.subList(index, indices.size()));
			return;
		}
		int[] singleton = new int[indices.size() - index];
		singleton[0] = 1;
		for (int i = 1; i < singleton.length; i++) {
			singleton[i] = indices.get(index + i);
		}
		putSucc(key, singleton);
		updateCoCs(1, count);
		for (int i = index + 1; i <= indices.size(); i++) {
			this.updateNCounts(i, 1, count);
		}
	}

	void updateCount(boolean count, boolean terminal) {
		if (count) this.counts[0]++; else this.counts[0]--;
		if (!terminal) {
			if (count) this.counts[1]++; else this.counts[1]--;
		}
	}

	private void updateCoCs(int count, boolean added) {
		if (COUNT_OF_COUNTS_CUTOFF == 0) return;
		int currIndex = Math.min(count, COUNT_OF_COUNTS_CUTOFF);
		int prevIndex = Math.min(count + (added ? -1 : 1), COUNT_OF_COUNTS_CUTOFF);
		if (currIndex != prevIndex) {
			if (currIndex >= 1) this.counts[currIndex + 1]++;
			if (prevIndex >= 1) this.counts[prevIndex + 1]--;
		}
	}

	void updateNCounts(int n, int count, boolean added) {
		if (n == 0) return;
		if (count > 0 && count < 5) {
			TrieCounterData.nCounts[n - 1][count - 1]++;
		}
		int prevCount = added ? count - 1 : count + 1;
		if (prevCount > 0 && prevCount < 5) {
			TrieCounterData.nCounts[n - 1][prevCount - 1]--;
		}
	}

	/*
	 * Map bookkeeping
	 */
	private int getSuccIx(int key) {
		if (this.indices.length < 1000) {
			return Arrays.binarySearch(this.indices, key);
		}
		else {
			int high = this.indices.length - 1;
			int guess = key >= high ? high >>> 1 : key;
			int ix = binSearch(key, 0, high, guess);
			return ix;
		}
	}

	// Binary search with initial guess based on sorted property of indices, otherwise based on java.util.Arrays.binarySearch0
	private int binSearch(int key, int low, int high, int mid) {
        while (low <= high) {
            int midVal = this.indices[mid];
            if (midVal == key) return mid;
            if (midVal < key) low = mid + 1;
            else high = mid - 1;
            mid = (low + high) >>> 1;
        }
        return -(low + 1);  // key not found.
	}

	Object getSuccessor(Integer key) {
		int ix = getSuccIx(key);
		if (ix < 0) {
			return null;
		}
		else return this.successors[ix];
	}

	private void removeSucc(int index) {
		int ix = getSuccIx(index);
		if (ix >= 0) {
			if (ix < this.indices.length - 1) {
				System.arraycopy(this.indices, ix + 1, this.indices, ix, this.indices.length - ix - 1);
				System.arraycopy(this.successors, ix + 1, this.successors, ix, this.successors.length - ix - 1);
			}
			this.indices[this.indices.length - 1] = Integer.MAX_VALUE;
			int padding = getSuccIx(Integer.MAX_VALUE);
			if (padding >= 5 && padding < this.indices.length / 2) {
				this.indices = Arrays.copyOf(this.indices, padding + 1);
				this.successors = Arrays.copyOf(this.successors, padding + 1);
			}
		}
	}

	private void putSucc(Integer key, Object o) {
		int ix = getSuccIx(key);
		if (ix >= 0) {
			this.successors[ix] = o;
		} else {
			ix = -ix - 1;
			if (ix >= this.indices.length) grow();
			if (this.indices[ix] != Integer.MAX_VALUE) {
				System.arraycopy(this.indices, ix, this.indices, ix + 1, this.indices.length - ix - 1);
				System.arraycopy(this.successors, ix, this.successors, ix + 1, this.successors.length - ix - 1);
			}
			this.indices[ix] = key;
			this.successors[ix] = o;
			if (this.indices[this.indices.length - 1] != Integer.MAX_VALUE) {
				grow();
			}
		}
	}

	private void grow() {
		int oldLen = this.indices.length;
		this.indices = Arrays.copyOf(this.indices, (int) (this.indices.length * GROWTH_FACTOR + 1));
		this.successors = Arrays.copyOf(this.successors, (int) (this.successors.length * GROWTH_FACTOR + 1));
		for (int i = oldLen; i < this.indices.length; i++) this.indices[i] = Integer.MAX_VALUE;
	}
}