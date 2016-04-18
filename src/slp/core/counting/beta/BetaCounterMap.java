package slp.core.counting.beta;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class BetaCounterMap extends BetaCounter {

	private static final int COUNT_OF_COUNTS_CUTOFF = 5;
	private Map<Integer, BetaCounter> successors;
	private Multimap<Integer, BetaCounter> countsMap;

	public BetaCounterMap() {
		super();
		this.successors = new HashMap<Integer, BetaCounter>();
		this.countsMap = HashMultimap.create();
	}
	
	@Override
	public int getDistinctSuccessors() {
		return this.successors.size();
	}

	@Override
	public boolean update(List<Integer> indices, int index, boolean count) {
		if (index < indices.size()) {
			Integer key = indices.get(index);
			BetaCounter next = getOrCreateSuccessor(key);
			boolean success = next.update(indices, index + 1, count);
			if (!success) {
				// If can't update next, promote next and retry
				next = promote(key);
				return update(indices, index, count);
			}
			if (index == indices.size() - 1) {
				updateMaps(next, key, count);
			}
		}
		else {
			this.updateCount(count);
			this.updateNCounts(indices.size(), this.getCount(), count);
		}
		// Always successful
		return true;
	}
	
	private void updateMaps(BetaCounter successor, Integer index, boolean added) {
		// Update new count stats
		int count = successor.getCount();
		if (count == 0) {
			this.successors.remove(index);
			// If old > 0, remove its count
			if (!added) {
				this.countsMap.remove(count + 1, successor);
			}
		}
		else if (count < COUNT_OF_COUNTS_CUTOFF || (added && count == COUNT_OF_COUNTS_CUTOFF)) {
			this.countsMap.remove(count + (added ? -1 : 1), successor);
			this.countsMap.put(count, successor);
		}
	}

	@Override
	protected BetaCounter getOrCreateSuccessor(Integer index) {
		// TODO: this is somewhat costly across many calls (~1 microsecond/invoc.).
		BetaCounter next = getSuccessor(index);
		if (next != null) return next;
		else {
			BetaCounter value = new BetaCounterSingles();
			this.successors.put(index, value);
			return value;
		}
	}

	@Override
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
			int otherCounts = this.countsMap.size();
			for (int i = 1; i < range; i++) {
				int countI = this.countsMap.get(i).size();
				distinctCounts[i - 1] = countI;
				otherCounts -= countI;
			}
			distinctCounts[range - 1] = otherCounts;
			return distinctCounts;
		}
	}

	private BetaCounter promote(Integer key) {
		BetaCounter curr = this.successors.get(key);
		if (curr instanceof BetaCounterSingles) {
			return promoteSinglesToArray(key, (BetaCounterSingles) curr);
		} else if (curr instanceof BetaCounterArray) {
			return promoteArrToMap(key, (BetaCounterArray) curr);
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
		this.successors.put(key, newNext);
		if (newNext.count > 0) {
			this.countsMap.remove(newNext.count, curr);
			this.countsMap.put(newNext.count, newNext);
		}
		return newNext;
	}

	@SuppressWarnings("unused")
	private BetaCounter promoteSinglesToMap(Integer key, BetaCounterSingles curr) {
		BetaCounterMap newNext = new BetaCounterMap();
		newNext.count = curr.getCount();
		// Transfer old counter values into new counter
		if (curr.successor1Index > BetaCounterSingles.NONE) {
			BetaCounterSingles next1 = new BetaCounterSingles();
			next1.count = curr.successor1Count;
			newNext.successors.put(curr.successor1Index, next1);
			if (curr.successor2Index > BetaCounterSingles.NONE) {
				BetaCounterSingles next2 = new BetaCounterSingles();
				next2.count = curr.successor2Count;
				newNext.successors.put(curr.successor2Index, next2);
				if (curr.successor2Index > BetaCounterSingles.NONE) {
					BetaCounterSingles next3 = new BetaCounterSingles();
					next3.count = curr.successor3Count;
					newNext.successors.put(curr.successor3Index, next3);
				}
			}
		}
		// Update maps
		this.successors.put(key, newNext);
		if (newNext.count > 0) {
			this.countsMap.remove(newNext.count, curr);
			this.countsMap.put(newNext.count, newNext);
		}
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
		}
		curr.array = null;
		curr.indices = null;
		this.successors.put(key, newNext);
		if (newNext.count > 0) {
			this.countsMap.remove(newNext.count, curr);
			this.countsMap.put(newNext.count, newNext);
		}
		return newNext;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}

}
