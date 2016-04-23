package slp.core.counting.beta;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import slp.core.util.Configuration;

import java.util.Set;

public class BetaCounterMap extends BetaCounter {

	private static final int COUNT_OF_COUNTS_CUTOFF = 10;
	private Map<Integer, BetaCounter> successors;
	private Set<BetaCounter>[] countsArray;

	@SuppressWarnings("unchecked")
	public BetaCounterMap() {
		super();
		this.successors = new HashMap<Integer, BetaCounter>();
		this.countsArray = new Set[COUNT_OF_COUNTS_CUTOFF];
		for (int i = 0; i < this.countsArray.length; i++) {
			this.countsArray[i] = new HashSet<BetaCounter>();
		}
	}
	
	@Override
	public int getDistinctSuccessors() {
		return this.successors.size();
	}

	@Override
	public boolean update(List<Integer> indices, int index, boolean count, boolean fast) {
		if (index < indices.size()) {
			Integer key = indices.get(index);
			BetaCounter next = getOrCreateSuccessor(key, index, indices.size());
			boolean success = next.update(indices, index + 1, count, fast);
			if (!success) {
				// If can't update next, promote next and retry
				next = promote(key);
				return update(indices, index, count, fast);
			}
			if (fast || index == indices.size() - 1) {
				updateMaps(next, key, count);
			}
		}
		if (fast || index == indices.size()) {
			this.updateCount(count);
			this.updateNCounts(index, this.getCount(), count);
		}
		// Always successful
		return true;
	}
	
	private void updateMaps(BetaCounter successor, Integer index, boolean added) {
		// Update new count stats
		int count = successor.getCount();
		int currIndex = Math.min(count, COUNT_OF_COUNTS_CUTOFF) - 1;
		int prevIndex = Math.min(count + (added ? -1 : 1), COUNT_OF_COUNTS_CUTOFF) - 1;
		if (currIndex != prevIndex) {
			if (prevIndex >= 0) this.countsArray[prevIndex].remove(successor);
			if (currIndex >= 0) this.countsArray[currIndex].add(successor);
		}
		if (count == 0) {
			this.successors.remove(index);
		}
	}

	protected BetaCounter getOrCreateSuccessor(Integer key, int currIndex, int sequenceLength) {
		BetaCounter next = getSuccessor(key);
		if (next != null) return next;
		else {
			BetaCounter value = new BetaCounterSingle();
			this.successors.put(key, value);
			return value;
		}
	}

	protected BetaCounter getSuccessor(Integer index) {
		return this.successors.get(index);
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
			int[] distinctCounts = new int[range];
			int totalCounts = this.successors.size();
			for (int i = 1; i < range; i++) {
				int countOfCountsI = this.countsArray[i - 1].size();
				distinctCounts[i - 1] = countOfCountsI;
				totalCounts -= countOfCountsI;
			}
			distinctCounts[range - 1] = totalCounts;
			return distinctCounts;
		}
	}

	/*
	 * A map promotes successors from Single to Array to Map
	 */
	private BetaCounter promote(Integer key) {
		BetaCounter curr = this.successors.get(key);
		if (curr instanceof BetaCounterSingle) {
			return promoteSingleToSmall(key, (BetaCounterSingle) curr);
		} else if (curr instanceof BetaCounterSmall) {
			return promoteSmallToArray(key, (BetaCounterSmall) curr);
		} else if (curr instanceof BetaCounterArray) {
			return promoteArrToMap(key, (BetaCounterArray) curr);
		}
		return curr;
	}

	private BetaCounter promoteSingleToSmall(Integer key, BetaCounterSingle curr) {
		BetaCounterSmall newNext = new BetaCounterSmall();
		newNext.count = curr.count;
		newNext.successor1 = curr.successor1;
		newNext.successor1Index = curr.successor1Index;
		updateMapsAfterPromotion(key, curr, newNext);
		return newNext;
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
		updateMapsAfterPromotion(key, curr, newNext);
		return newNext;
	}

	private BetaCounter promoteArrToMap(Integer key, BetaCounterArray curr) {
		BetaCounterMap newNext = new BetaCounterMap();
		newNext.count = curr.getCount();
		for (int i = 0; i < curr.indices.length; i++) {
			int index = curr.indices[i];
			if (index == BetaCounterArray.NONE) break;
			BetaCounter successor = curr.array[i];
			newNext.successors.put(index, successor);
			newNext.countsArray[Math.min(COUNT_OF_COUNTS_CUTOFF, successor.count) - 1].add(successor);
		}
		curr.array = null;
		curr.indices = null;
		updateMapsAfterPromotion(key, curr, newNext);
		return newNext;
	}

	/*
	 * "Fast-track" method, experimentally doesn't seem successful.
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
		updateMapsAfterPromotion(key, curr, newNext);
		return newNext;
	}

	/*
	 * "Fast-track" method, experimentally doesn't seem successful.
	 */
	@SuppressWarnings("unused")
	private BetaCounter promoteSingleToMap(Integer key, BetaCounterSingle curr) {
		BetaCounterMap newNext = new BetaCounterMap();
		newNext.count = curr.count;
		if (curr.successor1Index != BetaCounterSingle.NONE) {
			newNext.successors.put(curr.successor1Index, curr.successor1);
		}
		updateMapsAfterPromotion(key, curr, newNext);
		return newNext;
	}

	/*
	 * "Fast-track" method, experimentally doesn't seem successful.
	 */
	@SuppressWarnings("unused")
	private BetaCounter promoteSmallToMap(Integer key, BetaCounterSmall curr) {
		BetaCounterMap newNext = new BetaCounterMap();
		newNext.count = curr.count;
		if (curr.successor1Index != BetaCounterSingle.NONE) {
			newNext.successors.put(curr.successor1Index, curr.successor1);
			if (curr.successor2Index != BetaCounterSingle.NONE) {
				newNext.successors.put(curr.successor2Index, curr.successor2);
				if (curr.successor3Index != BetaCounterSingle.NONE) {
					newNext.successors.put(curr.successor3Index, curr.successor3);
				}
			}
		}
		updateMapsAfterPromotion(key, curr, newNext);
		return newNext;
	}

	private void updateMapsAfterPromotion(Integer key, BetaCounter curr, BetaCounter newNext) {
		this.successors.put(key, newNext);
		if (newNext.count > 0) {
			int index = Math.min(newNext.count, COUNT_OF_COUNTS_CUTOFF) - 1;
			this.countsArray[index].remove(curr);
			this.countsArray[index].add(newNext);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(0);
		out.writeInt(this.count);
		out.writeInt(this.successors.size());
		for (Entry<Integer, BetaCounter> entry : this.successors.entrySet()) {
			out.writeInt(entry.getKey());
			entry.getValue().writeExternal(out);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.count = in.readInt();
		// Special case: if count is 0, this must be the root and we read type instead (can't know the first time)
		if (this.count == 0) {
			BetaCounter.depthForReadingIn = 0;
			this.count = in.readInt();
			BetaCounter.nCounts = new int[Configuration.order()][4];
		} else {
			BetaCounter.nCounts[depthForReadingIn - 1][Math.min(this.count, 4) - 1]++;
		}
		readSuccessors(in);
		initCountsArray();
	}

	private void readSuccessors(ObjectInput in) throws IOException, ClassNotFoundException {
		int size = in.readInt(); 
		if (size == 0) return;
		this.successors = new HashMap<Integer, BetaCounter>(size, 1.00f);
		for (int i = 0; i < size; i++) {
			int index = in.readInt();
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
			this.successors.put(index, counter);
		}
	}

	@SuppressWarnings("unchecked")
	private void initCountsArray() {
		this.countsArray = new Set[COUNT_OF_COUNTS_CUTOFF];
		for (int i = 0; i < this.countsArray.length; i++) {
			this.countsArray[i] = new HashSet<BetaCounter>();
		}
		for (BetaCounter counter : this.successors.values()) {
			int index = Math.min(counter.getCount(), COUNT_OF_COUNTS_CUTOFF) - 1;
			this.countsArray[index].add(counter);
		}
	}
}
