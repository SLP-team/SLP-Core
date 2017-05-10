package slp.core.counting.trie;

import java.util.Arrays;
import java.util.List;

import slp.core.modeling.ModelRunner;

public class TrieCounterData {
	/**
	 * 'counts' contains in order: own count, context count (sum of successor's counts),
	 * no of distinct successors seen once, twice, up to the COCcutoff in Configuration
	 */
	public int[] counts;
	public int[] indices;
	public Object[] successors;
	
	public static int COUNT_OF_COUNTS_CUTOFF = 3;
	public static int[][] nCounts = new int[ModelRunner.getNGramOrder()][4];
	private static final double GROWTH_FACTOR = 1.1;

	public TrieCounterData(int initSize) {
		this.counts = new int[2 + COUNT_OF_COUNTS_CUTOFF];
		this.indices = new int[initSize];
		this.successors = new Object[initSize];
		Arrays.fill(this.indices, Integer.MAX_VALUE);
	}

	void updateSuccessor(List<Integer> indices, int index, int adj, Object succ) {
		if (succ instanceof TrieCounter) updateTrie(indices, index, adj, succ);
		else updateArray(indices, index, adj, succ);
	}

	private void updateTrie(List<Integer> indices, int index, int adj, Object succ) {
		TrieCounter next = (TrieCounter) succ;
		next.update(indices, index + 1, adj);
		updateCoCs(next.getCount(), adj);
		if (next.getCount() == 0) {
			removeSucc(indices.get(index));
		}
	}

	private void updateArray(List<Integer> indices, int index, int adj, Object succ) {
		int[] successor = (int[]) succ;
		boolean valid = ArrayStorage.checkExactSequence(indices, index, successor);
		if (valid) updateArrayCount(indices, index, adj, successor);
		else {
			if (adj < 0) System.err.println("Attempting to unsee never seen event");
			TrieCounter newNext = promoteArrayToTrie(indices, index, successor);
			updateTrie(indices, index, adj, newNext);
		}
	}

	private void updateArrayCount(List<Integer> indices, int index, int adj, int[] successor) {
		successor[0] += adj;
		updateCoCs(successor[0], adj);
		if (successor[0] == 0) {
			removeSucc(indices.get(index));
		}
		for (int i = index + 1; i <= indices.size(); i++) {
			updateNCounts(i, successor[0], adj);
		}
	}

	private TrieCounter promoteArrayToTrie(List<Integer> indices, int index, int[] successor) {
		TrieCounter newNext = new TrieCounter(1);
		newNext.data.counts[0] = successor[0];
		if (successor.length > 1) {
			newNext.data.counts[1] = newNext.data.counts[0];
			int[] temp = Arrays.copyOfRange(successor, 1, successor.length);
			temp[0] = successor[0];
			newNext.data.putSucc(successor[1], temp);
			if (COUNT_OF_COUNTS_CUTOFF > 0) newNext.data.counts[1 + Math.min(temp[0], COUNT_OF_COUNTS_CUTOFF)]++;
		}
		putSucc(indices.get(index), newNext);
		return newNext;
	}

	void addSucessor(List<Integer> indices, int index, int adj) {
		if (adj < 0) {
			System.out.println("Attempting to store new event with negative count: " + indices.subList(index, indices.size()));
			return;
		}
		int[] singleton = new int[indices.size() - index];
		singleton[0] = adj;
		for (int i = 1; i < singleton.length; i++) {
			singleton[i] = indices.get(index + i);
		}
		putSucc(indices.get(index), singleton);
		updateCoCs(adj, adj);
		for (int i = index + 1; i <= indices.size(); i++) {
			updateNCounts(i, adj, adj);
		}
	}

	void updateCount(int adj, boolean terminal) {
		this.counts[0] += adj;
		if (!terminal) this.counts[1] += adj;
	}

	private void updateCoCs(int count, int adj) {
		if (COUNT_OF_COUNTS_CUTOFF == 0) return;
		int currIndex = Math.min(count, COUNT_OF_COUNTS_CUTOFF);
		int prevIndex = Math.min(count - adj, COUNT_OF_COUNTS_CUTOFF);
		if (currIndex != prevIndex) {
			if (currIndex >= 1) this.counts[currIndex + 1]++;
			if (prevIndex >= 1) this.counts[prevIndex + 1]--;
		}
	}

	public static synchronized void updateNCounts(int n, int count, int adj) {
		if (n == 0) return;
		int[] toUpdate = nCounts[n - 1];
		int currIndex = Math.min(count, toUpdate.length);
		int prevIndex = Math.min(count - adj, toUpdate.length);
		if (currIndex != prevIndex) {
			if (currIndex > 0) toUpdate[currIndex - 1]++;
			if (prevIndex > 0) toUpdate[prevIndex - 1]--;
		}
	}

	/*
	 * Map bookkeeping
	 */
	private int getSuccIx(int key) {
		// Quickly check if key is stored at its purely sequential location; the 'root' trie is usually
		// populated with the whole vocabulary in order up to some point, so a quick guess can save time.
		if (key > 0 && key < 1000 && key <= this.indices.length && this.indices[key - 1] == key) return key - 1;
		// Otherwise, binary search will do
		return Arrays.binarySearch(this.indices, key);
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
		int newLen = (int) (this.indices.length * GROWTH_FACTOR + 1);
		if (newLen == oldLen - 1) newLen++;
		this.indices = Arrays.copyOf(this.indices, newLen);
		this.successors = Arrays.copyOf(this.successors, newLen);
		for (int i = oldLen; i < this.indices.length; i++) this.indices[i] = Integer.MAX_VALUE;
	}
}