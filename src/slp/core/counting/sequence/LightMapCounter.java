package slp.core.counting.sequence;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Configuration;

public class LightMapCounter implements Counter {

	private static int[][] nCounts = new int[Configuration.order()][4];
	private static final int COUNT_OF_COUNTS_CUTOFF = 3;
	private static final double GROWTH_FACTOR = 1.1;
	
	/*
	 * 'counts' contains in order: own count, context count (sum of successor's counts),
	 * no of distinct successors seen once, twice, up to the CUTOFF above
	 */
	private int[] counts;
	private int[] succIX;
	private Object[] succs;

	public LightMapCounter() {
		this(1);
	}
	
	public LightMapCounter(int initSize) {
		this.counts = new int[2 + COUNT_OF_COUNTS_CUTOFF];
		this.succIX = new int[initSize];
		this.succs = new Object[initSize];
		Arrays.fill(this.succIX, Integer.MAX_VALUE);
	}
	
	public int[] getSuccIXs() {
		return this.succIX;
	}

	@Override
	public int getCount() {
		return this.counts[0];
	}
	
	@Override
	public final void update(List<Integer> indices, boolean count) {
		update(indices, 0, count, false);
	}

	@Override
	public final void updateForward(List<Integer> indices, boolean count) {
		update(indices, 0, count, true);
	}

	public void update(List<Integer> indices, int index, boolean count, boolean fast) {
		if (index < indices.size()) {
			Integer key = indices.get(index);
			Object succ = getSucc(key);
			if (succ != null) {
				if (succ instanceof LightMapCounter) {
					LightMapCounter next = (LightMapCounter) succ;
					next.update(indices, index + 1, count, fast);
					if (fast || index == indices.size() - 1) {
						updateCoCs(next.getCount(), count);
					}
					if (next.getCount() == 0) {
						removeSucc(key);
					}
				}
				else {
					int[] successor = (int[]) succ;
					boolean valid = checkExactSequence(indices, index, successor);
					if (valid) {
						successor[0] += (count ? -1 : 1);
						if (fast || index == indices.size() - 1) {
							updateCoCs(-successor[0], count);
						}
						if (successor[0] == 0) {
							removeSucc(key);
						}
						for (int i = index + 1; i <= indices.size(); i++) {
							this.updateNCounts(i, -successor[0], count);
						}
					}
					else {
						if (!count) System.err.println("Attempting to unsee never seen event");
						LightMapCounter newNext = new LightMapCounter(1);
						newNext.counts[0] = -successor[0];
						if (successor.length > 1) {
							newNext.counts[1] = newNext.counts[0];
							int[] temp = Arrays.copyOfRange(successor, 1, successor.length);
							temp[0] = successor[0];
							newNext.putSucc(successor[1], temp);
							newNext.counts[1 + Math.min(-temp[0], COUNT_OF_COUNTS_CUTOFF)]++;
						}
						putSucc(key, newNext);
						update(indices, index, count, fast);
						return;
					}
				}
			}
			else if (!count) {
				System.out.println("Attempting to unsee never seen event: " + key + "\t" + indices.subList(index, indices.size()));
			}
			else if (!fast) {
				putSucc(key, new LightMapCounter(1));
				update(indices, index, count, fast);
				return;
			}
			else {
				int[] singleton = new int[indices.size() - index];
				singleton[0] = -1;
				for (int i = 1; i < singleton.length; i++) {
					singleton[i] = indices.get(index + i);
				}
				putSucc(key, singleton);
				if (fast || index == indices.size() - 1) {
					updateCoCs(1, count);
				}
				for (int i = index + 1; i <= indices.size(); i++) {
					this.updateNCounts(i, 1, count);
				}
			}
		}
		if (fast || index == indices.size()) {
			this.updateCount(count, index == indices.size());
			this.updateNCounts(index, this.getCount(), count);
		}
	}

	private void updateCount(boolean count, boolean terminal) {
		if (count) this.counts[0]++; else this.counts[0]--;
		if (!terminal) {
			if (count) this.counts[1]++; else this.counts[1]--;
		}
	}

	private void updateCoCs(int count, boolean added) {
		int currIndex = Math.min(count, COUNT_OF_COUNTS_CUTOFF);
		int prevIndex = Math.min(count + (added ? -1 : 1), COUNT_OF_COUNTS_CUTOFF);
		if (currIndex != prevIndex) {
			if (currIndex >= 1) this.counts[currIndex + 1]++;
			if (prevIndex >= 1) this.counts[prevIndex + 1]--;
		}
	}

	@Override
	public int getNCount(int n, int count) {
		return nCounts[n - 1][count - 1];
	}

	private void updateNCounts(int n, int count, boolean added) {
		if (n == 0) return;
		if (count > 0 && count < 5) {
			nCounts[n - 1][count - 1]++;
		}
		int prevCount = added ? count - 1 : count + 1;
		if (prevCount > 0 && prevCount < 5) {
			nCounts[n - 1][prevCount - 1]--;
		}
	}

	public Object getNode(List<Integer> indices) {
		return getSuccessor(indices, 0);
	}

	private Object getSuccessor(List<Integer> indices, int index) {
		if (index == indices.size()) return this;
		Integer next = indices.get(index);
		Object succ = getSucc(next);
		if (succ == null) return null;
		else if (succ instanceof LightMapCounter) {
			LightMapCounter successor = (LightMapCounter) succ;
			return successor.getSuccessor(indices, index + 1);
		}
		else {
			int[] successor = (int[]) succ;
			if (!checkPartialSequence(indices, index, successor)) return null;
			int[] trueSucc = new int[1 + successor.length - (indices.size() - index)];
			trueSucc[0] = successor[0];
			for (int i = 1; i < trueSucc.length; i++) {
				trueSucc[i] = successor[i + indices.size() - index - 1];
			}
			return trueSucc;
		}
	}

	@Override
	public int[] getShortCounts(List<Integer> indices) {
		return getShortCounts(indices, 0);
	}

	private int[] getShortCounts(List<Integer> indices, int index) {
		Integer next = indices.get(index);
		Object succ = getSucc(next);//this.successors.get(next);
		int[] counts = new int[2];
		boolean nearLast = index == indices.size() - 1;
		if (nearLast) counts[1] = this.counts[1];
		if (succ != null) {
			if (succ instanceof LightMapCounter) {
				LightMapCounter successor = (LightMapCounter) succ;
				if (!nearLast) return successor.getShortCounts(indices, index + 1);
				counts[0] = successor.getCount();
			}
			else {
				int[] successor = (int[]) succ;
				if (checkPartialSequence(indices, index, successor)) {
					counts[0] = -successor[0];
					if (!nearLast) counts[1] = counts[0];
				}
				else if (!nearLast && successor.length >= indices.size() - index
						&& checkPartialSequence(indices.subList(0, indices.size() - 1), index, successor)) {
					counts[1] = -successor[0];
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
			Object succ = getSucc(next);//this.successors.get(next);
			if (succ == null) return new int[range];
			if (succ instanceof LightMapCounter) {
				LightMapCounter successor = (LightMapCounter) succ;
				return successor.getDistinctCounts(range, indices, index + 1);
			}
			else {
				int[] successor = (int[]) succ;
				int[] distinctCounts = new int[range];
				if (checkPartialSequence(indices, index, successor) && !checkExactSequence(indices, index, successor)) {
					distinctCounts[Math.min(range - 1, -successor[0] - 1)]++;
				}
				return distinctCounts;
			}
		} else {
			int[] distinctCounts = new int[range];
			int totalDistinct = this.getDistinctSuccessors();
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
	public int getDistinctSuccessors() {
		return Arrays.stream(this.counts).skip(2).sum();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.counts.length);
		for (int i = 0; i < this.counts.length; i++) out.writeInt(this.counts[i]);
		out.writeInt(this.getDistinctSuccessors());
		for (int i = 0; i < this.succIX.length; i++) {
			if (this.succIX[i] == Integer.MAX_VALUE) continue;
			out.writeInt(this.succIX[i]);
			Object o = this.succs[i];
			if (o instanceof int[]) {
				int[] arr = (int[]) o;
				out.writeInt(arr.length);
				for (int j = 0; j < arr.length; j++) out.writeInt(arr[j]);
			}
			else {
				out.writeInt(-1);
				out.writeObject((LightMapCounter) o);
			}
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int len = in.readInt();
		this.counts = new int[len];
		for (int i = 0; i < len; i++) this.counts[i] = in.readInt();
		int successors = in.readInt();
		this.succIX = new int[successors];
		this.succs = new Object[successors];
		int pos = 0;
		for (; pos < successors; pos++) {
			int key = in.readInt();
			int code = in.readInt();
			Object value;
			if (code < 0) {
				value = in.readObject();
			}
			else {
				value = new int[code];
				for (int j = 0; j < code; j++) ((int[]) value)[j] = in.readInt();
			}
			this.succIX[pos] = key;
			this.succs[pos] = value;
		}
	}

	/*
	 * Singleton storage
	 */
	private boolean checkExactSequence(List<Integer> indices, int index, int[] successor) {
		boolean valid = successor.length == indices.size() - index;
		if (valid) {
			for (int i = 1; i < successor.length; i++) {
				if (indices.get(index + i) != successor[i]) {
					valid = false;
					break;
				}
			}
		}
		return valid;
	}

	@SuppressWarnings("unused")
	private int checkSequence(List<Integer> indices, int index, int[] successor, boolean exact) {
		for (int i = 0; i < successor.length; i++) {
			if (successor[i] < 0) {
				if (successor.length - i < indices.size() - index) break;
				for (int j = i + 1; j < successor.length; j++) {
					int offset = index + (j - i);
					if (successor[j] < 0) {
						if (offset == indices.size()) return i;
						else break;
					}
					if (offset == indices.size()) {
						if (!exact) return i;
						else break;
					}
					if (!indices.get(offset).equals(successor[j])) break;
				}
			}
		}
		return -1;
	}

	private boolean checkPartialSequence(List<Integer> indices, int index, int[] successor) {
		boolean valid = successor.length >= indices.size() - index;
		if (valid) {
			for (int i = 1; i < indices.size() - index; i++) {
				if (indices.get(index + i) != successor[i]) {
					valid = false;
					break;
				}
			}
		}
		return valid;
	}

	/*
	 * Map bookkeeping
	 */
	private int getSuccIx(int key) {
		if (this.succIX.length < 10000) {
			return Arrays.binarySearch(this.succIX, key);
		}
		else {
			int high = this.succIX.length;
			int guess = key >= high ? high >>> 1 : key;
			int ix = binSearch(key, 0, guess, high);
			return ix;
		}
	}

	// Binary search with initial values, otherwise based on java.util.Arrays.binarySearch0
	private int binSearch(int key, int low, int midGuess, int high) {
		while (low <= high) {
			int midVal = this.succIX[midGuess];
			if (midVal < key) low = midGuess + 1;
			else if (midVal > key) high = midGuess - 1;
			else return midGuess; // key found
			midGuess = (low + high) >>> 1;
		}
		return -(low + 1); // key not found.
	}
	
	private Object getSucc(Integer key) {
		int ix = getSuccIx(key);
		if (ix < 0) {
			return null;
		}
		else return this.succs[ix];
	}

	private void removeSucc(int index) {
		int ix = getSuccIx(index);
		if (ix >= 0) {
			if (ix < this.succIX.length - 1) {
//				for (int i = ix; i < this.succIX.length - 1; i++) {
//					this.succIX[i] = this.succIX[i + 1];
//					this.succs[i] = this.succs[i + 1];
//					if (this.succIX[i] == Integer.MAX_VALUE) break;
//				}
				System.arraycopy(this.succIX, ix + 1, this.succIX, ix, this.succIX.length - ix - 1);
				System.arraycopy(this.succs, ix + 1, this.succs, ix, this.succs.length - ix - 1);
			}
			this.succIX[this.succIX.length - 1] = Integer.MAX_VALUE;
			int padding = getSuccIx(Integer.MAX_VALUE);
			if (padding >= 5 && padding < this.succIX.length / 2) {
				this.succIX = Arrays.copyOf(this.succIX, padding + 1);
				this.succs = Arrays.copyOf(this.succs, padding + 1);
			}
		}
	}

	private void putSucc(Integer key, Object o) {
		int ix = getSuccIx(key);
		if (ix >= 0) {
			this.succs[ix] = o;
		} else {
			ix = -ix - 1;
			if (ix >= this.succIX.length) grow();
			if (this.succIX[ix] != Integer.MAX_VALUE) {
//				for (int i = this.succIX.length - 2; i >= ix; i--) {
//					if (this.succIX[i] == Integer.MAX_VALUE) continue;
//					this.succIX[i + 1] = this.succIX[i];
//					this.succs[i + 1] = this.succs[i];
//				}
				System.arraycopy(this.succIX, ix, this.succIX, ix + 1, this.succIX.length - ix - 1);
				System.arraycopy(this.succs, ix, this.succs, ix + 1, this.succs.length - ix - 1);
			}
			this.succIX[ix] = key;
			this.succs[ix] = o;
			if (this.succIX[this.succIX.length - 1] != Integer.MAX_VALUE) {
				grow();
			}
		}
	}

	private void grow() {
		int oldLen = this.succIX.length;
		this.succIX = Arrays.copyOf(this.succIX, (int) (this.succIX.length * GROWTH_FACTOR + 1));
		this.succs = Arrays.copyOf(this.succs, (int) (this.succs.length * GROWTH_FACTOR + 1));
		for (int i = oldLen; i < this.succIX.length; i++) this.succIX[i] = Integer.MAX_VALUE;
	}
}
