package slp.core.counting.beta;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;

public class BetaCounterArray extends BetaCounter {

	public static final int NONE = (int) -1E10;
	private static final int INIT_SIZE = 4;
	private static final int MAX_SIZE = 64*INIT_SIZE;
	
	protected int[] indices;
	protected BetaCounter[] array;
	
	public BetaCounterArray() {
		super();
		this.indices = new int[INIT_SIZE];
		this.array = new BetaCounter[INIT_SIZE];
		Arrays.fill(this.indices, NONE);
	}
	
	@Override
	public int getDistinctSuccessors() {
		for (int i = 0; i < this.array.length; i++) {
			if (this.array[i] == null) return i;
		}
		return this.array.length;
	}

	@Override
	public boolean update(List<Integer> indices, int index, boolean count) {
		// Cannot accommodate successors two steps ahead
		if (index < indices.size()) {
			Integer key = indices.get(index);
			int successorIndex = getOrCreateSuccessorIndex(key);
			// Out of capacity
			if (successorIndex < 0) {
				return false;
			}
			// Try to update the successor
			BetaCounter successor = this.array[successorIndex];
			boolean success = successor.update(indices, index + 1, count);
			if (!success) {
				BetaCounter newSuccessor = promote(key);
				// cannot promote successor past Array
				if (newSuccessor == null) return false;
				this.array[successorIndex] = newSuccessor;
				return update(indices, index, count);
			}
			// If a successor hits zero, remove it
			else if (successor.getCount() == 0) {
				removeSuccessor(key);
			}
		}
		else {
			this.updateCount(count);
		}
		return true;
	}

	@Override
	protected BetaCounter getOrCreateSuccessor(Integer key) {
		int successorIndex = getOrCreateSuccessorIndex(key);
		if (successorIndex < 0) return null;
		else return this.array[successorIndex];
	}
	
	protected int getOrCreateSuccessorIndex(Integer key) {
		int nextIndex = getSuccessorIndex(key);
		if (nextIndex >= 0) return nextIndex;
		else if (nextIndex == NONE) return -1;
		// If getSuccessorIndex fails to find the key, it returns the negative of the first open slot minus 1
		else {
			int index = -(nextIndex + 1);
			BetaCounter value = new BetaCounterSingles();
			this.indices[index] = key;
			this.array[index] = value;
			return index;
		}
	}

	@Override
	protected BetaCounter getSuccessor(Integer key) {
		int index = getSuccessorIndex(key);
		if (index < 0) return null;
		else return this.array[index];
	}
	
	protected int getSuccessorIndex(Integer key) {
		for (int i = 0; i < this.indices.length; i++) {
			if (this.indices[i] == key) {
				return i;
			}
			else if (this.indices[i] == NONE) {
				return -i - 1;
			}
		}
		return tryToGrow(key);
	}

	private int tryToGrow(Integer key) {
		if (this.indices.length < MAX_SIZE) {
			int length = this.indices.length;
			this.indices = Arrays.copyOf(indices, 2*length);
			this.array = Arrays.copyOf(array, 2*length);
			for (int i = length; i < this.indices.length; i++) {
				this.indices[i] = NONE;
			}
			return getSuccessorIndex(key);
		} else {
			return NONE;
		}
	}

	private void removeSuccessor(Integer key) {
		boolean update = false;
		for (int i = 0; i < this.indices.length - 1; i++) {
			if (this.indices[i] == key) {
				update = true;
			}
			if (update) {
				this.indices[i] = this.indices[i + 1];
				this.array[i] = this.array[i + 1];
			}
		}
		this.indices[this.indices.length - 1] = NONE;
		this.array[this.array.length - 1] = null;
	}

	@Override
	protected int[] getShortCounts(List<Integer> sequence, int index) {
		Integer next = sequence.get(index);
		BetaCounter successor = getSuccessor(next);
		if (index == sequence.size() - 1) {
			int[] counts = new int[2];
			counts[1] = this.count;
			if (successor != null) counts[0] = successor.getCount();
			return counts;
		}
		else if (successor != null) {
			return successor.getShortCounts(sequence, index + 1);
		}
		else {
			return new int[2];
		}
	}

	@Override
	protected int[] getDistinctCounts(int range, List<Integer> sequence, int index) {
		Integer next = sequence.get(index);
		BetaCounter successor = getSuccessor(next);
		if (index == sequence.size()) {
			int[] distinctCounts = new int[range];
			for (int i = 0; i < this.array.length; i++) {
				if (this.array[i] == null) break;
				else {
					int count = this.array[i].getCount();
					if (count > range) count = range;
					distinctCounts[count - 1]++;
				}
			}
			return distinctCounts;
		} else if (successor != null) {
			return successor.getDistinctCounts(range, sequence, index + 1);
		}
		else {
			return new int[range];
		}
	}

	private BetaCounter promote(Integer key) {
		BetaCounter curr = getSuccessor(key);
		if (curr instanceof BetaCounterSingles) {
			return promoteSinglesToArray(key, (BetaCounterSingles) curr);
		} else if (curr instanceof BetaCounterArray) {
			return null;
		}
		return curr;
	}
	
	private BetaCounter promoteSinglesToArray(Integer key, BetaCounterSingles curr) {
		BetaCounterArray newNext = new BetaCounterArray();
		newNext.count = curr.count;
		// Transfer old counter values into new counter
		if (curr.successor1Index > BetaCounterSingles.NONE) {
			BetaCounterSingles next1 = new BetaCounterSingles();
			next1.count = curr.successor1Count;
			newNext.indices[0] = curr.successor1Index;
			newNext.array[0] = next1;
			if (curr.successor2Index > BetaCounterSingles.NONE) {
				BetaCounterSingles next2 = new BetaCounterSingles();
				next2.count = curr.successor2Count;
				newNext.indices[1] = curr.successor2Index;
				newNext.array[1] = next2;
				if (curr.successor2Index > BetaCounterSingles.NONE) {
					BetaCounterSingles next3 = new BetaCounterSingles();
					next3.count = curr.successor3Count;
					newNext.indices[2] = curr.successor3Index;
					newNext.array[2] = next3;
				}
			}
		}
		return newNext;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

}
