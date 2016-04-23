package slp.core.counting.beta;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;

public class BetaCounterArray extends BetaCounter {

	public static final int NONE = (int) -1E10;
	private static final int INIT_SIZE = 16;
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
	public boolean update(List<Integer> indices, int index, boolean count, boolean fast) {
		// Cannot accommodate successors two steps ahead
		if (index < indices.size()) {
			// Reset cache
			this.distinctCache = null;
			Integer key = indices.get(index);
			int successorIndex = getOrCreateSuccessorIndex(key, index, indices.size());
			// Out of capacity
			if (successorIndex < 0) {
				return false;
			}
			// Try to update the successor
			BetaCounter successor = this.array[successorIndex];
			boolean success = successor.update(indices, index + 1, count, fast);
			if (!success) {
				BetaCounter newSuccessor = promote(key);
				// cannot promote successor past Array
				if (newSuccessor == null) return false;
				this.array[successorIndex] = newSuccessor;
				return update(indices, index, count, fast);
			}
			// If a successor hits zero, remove it
			if (successor.getCount() == 0) {
				removeSuccessor(key);
			}
		}
		if (fast || index == indices.size()) {
			this.updateCount(count);
			this.updateNCounts(index, this.getCount(), count);
		}
		return true;
	}
	
	protected int getOrCreateSuccessorIndex(Integer key, int currIndex, int sequenceLength) {
		int nextIndex = getSuccessorIndex(key);
		if (nextIndex >= 0) return nextIndex;
		else {
			// If getSuccessorIndex fails to find the key, it returns the negative of the first open slot minus 1 or NONE
			if (nextIndex == NONE) {
				nextIndex = tryToGrow(key);
				// Still NONE? Give up
				if (nextIndex == NONE) return -1;
			}
			int index = -(nextIndex + 1);
			BetaCounter value = new BetaCounterSingle();
			this.indices[index] = key;
			this.array[index] = value;
			return index;
		}
	}

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
		return NONE;
	}

	private int tryToGrow(Integer key) {
		if (this.indices.length < MAX_SIZE) {
			int length = this.indices.length;
			int newLength = Math.max(MAX_SIZE, 4*length);
			this.indices = Arrays.copyOf(indices, newLength);
			this.array = Arrays.copyOf(array, newLength);
			for (int i = length; i < this.indices.length; i++) {
				this.indices[i] = NONE;
			}
			return -length - 1;
		} else {
			return NONE;
		}
	}

	private void removeSuccessor(Integer key) {
		boolean update = false;
		for (int i = 0; i < this.indices.length - 1; i++) {
			if (!update && this.indices[i] == key) {
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

	private int[] distinctCache = null;
	@Override
	protected int[] getDistinctCounts(int range, List<Integer> sequence, int index) {
		if (index < sequence.size()) {
			Integer next = sequence.get(index);
			BetaCounter successor = getSuccessor(next);
			if (successor != null) {
				return successor.getDistinctCounts(range, sequence, index + 1);
			}
			else {
				return new int[range];
			}
		} else {
			// Use cache for faster lookup (large arrays are slow). Cache is reset on any update
			if (this.distinctCache != null && this.distinctCache.length == range) return this.distinctCache;
			int[] distinctCounts = new int[range];
			for (int i = 0; i < this.array.length; i++) {
				if (this.array[i] == null) break;
				int count = this.array[i].getCount();
				distinctCounts[Math.min(range, count) - 1]++;
			}
			this.distinctCache = distinctCounts;
			return distinctCounts;
		}
	}

	/*
	 * An array promotes successors from Single to Small to Array
	 */
	private BetaCounter promote(Integer key) {
		BetaCounter curr = getSuccessor(key);
		if (curr instanceof BetaCounterSingle) {
			return promoteSingleToSmall(key, (BetaCounterSingle) curr);
		} else if (curr instanceof BetaCounterSmall) {
			return promoteSmallToArray(key, (BetaCounterSmall) curr);
		} else if (curr instanceof BetaCounterArray) {
			return null;
		}
		return curr;
	}

	private BetaCounter promoteSmallToArray(Integer key, BetaCounterSmall curr) {
		BetaCounterArray newNext = new BetaCounterArray();
		newNext.count = curr.count;
		// Transfer old counter values into new counter
		if (curr.successor1Index != BetaCounterSmall.NONE) {
			newNext.indices[0] = curr.successor1Index;
			newNext.array[0] = curr.successor1;
			if (curr.successor2Index != BetaCounterSmall.NONE) {
				newNext.indices[1] = curr.successor2Index;
				newNext.array[1] = curr.successor2;
				if (curr.successor3Index != BetaCounterSmall.NONE) {
					newNext.indices[2] = curr.successor3Index;
					newNext.array[2] = curr.successor3;
				}
			}
		}
		return newNext;
	}

	/*
	 * "Fast-track" method
	 */
	@SuppressWarnings("unused")
	private BetaCounter promoteSingleToArray(Integer key, BetaCounterSingle curr) {
		BetaCounterArray newNext = new BetaCounterArray();
		newNext.count = curr.count;
		// Transfer old counter values into new counter
		if (curr.successor1Index != BetaCounterSingle.NONE) {
			newNext.indices[0] = curr.successor1Index;
			newNext.array[0] = curr.successor1;
		}
		return newNext;
	}

	private BetaCounter promoteSingleToSmall(Integer key, BetaCounterSingle curr) {
		BetaCounterSmall newNext = new BetaCounterSmall();
		newNext.count = curr.count;
		newNext.successor1 = curr.successor1;
		newNext.successor1Index = curr.successor1Index;
		return newNext;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(1);
		out.writeInt(this.count);
		int size = 0;
		for (int i = 0; i < this.array.length; i++) {
			if (this.array[i] == null) break;
			else size++;
		}
		out.writeInt(size);
		for (int i = 0; i < size; i++) {
			out.writeInt(this.indices[i]);
		}
		for (int i = 0; i < size; i++) {
			this.array[i].writeExternal(out);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.count = in.readInt();
		BetaCounter.nCounts[depthForReadingIn - 1][Math.min(this.count, 4) - 1]++;
		int size = in.readInt();
		this.indices = new int[size];
		this.array = new BetaCounter[size];
		for (int i = 0; i < size; i++) {
			this.indices[i] = in.readInt();
		}
		for (int i = 0; i < size; i++) {
			int type = in.readInt();
			BetaCounter counter;
			switch (type) {
				case 0: counter = new BetaCounterMap(); break;
				case 1: counter = new BetaCounterArray(); break;
				case 2: counter = new BetaCounterSmall(); break;
				case 3: counter = new BetaCounterSingle(); break;
				default: counter = new BetaCounterMap();
			}
			BetaCounter.depthForReadingIn++;
			counter.readExternal(in);
			BetaCounter.depthForReadingIn--;
			this.array[i] = counter;
		}
	}

}
