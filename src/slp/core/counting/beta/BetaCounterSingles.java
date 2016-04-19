package slp.core.counting.beta;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

public class BetaCounterSingles extends BetaCounter {

	public static final int NONE = (int) -1E10;
	protected int successor1Index = NONE;
	protected int successor1Count = 0;
	protected int successor2Index = NONE;
	protected int successor2Count = 0;
	protected int successor3Index = NONE;
	protected int successor3Count = 0;
	
	public BetaCounterSingles() {
		super();
	}
	
	@Override
	public int getDistinctSuccessors() {
		return this.successor1Index == NONE ? 0 :
				this.successor2Index == NONE ? 1 :
					this.successor3Index == NONE ? 2 : 3;
	}

	@Override
	public boolean update(List<Integer> indices, int index, boolean count, boolean fast) {
		// Cannot accommodate successors two steps ahead
		if (index < indices.size() - 1) return false;
		else if (index < indices.size()) {
			Integer key = indices.get(index);
			int updatedCount = 0;
			if (this.successor1Index == NONE) {
				if (!count) System.err.println("Attempt to remove unseen event");
				this.successor1Index = key;
				this.successor1Count = 1;
				updatedCount = this.successor1Count;
			}
			else if (this.successor1Index == key) {
				this.successor1Count += (count ? 1 : - 1);
				updatedCount = this.successor1Count;
				if (this.successor1Count == 0) {
					shift3();
				}
			}
			else if (this.successor2Index == NONE) {
				if (!count) System.err.println("Attempt to remove unseen event");
				this.successor2Index = key;
				this.successor2Count = 1;
				updatedCount = this.successor2Count;
			}
			else if (this.successor2Index == key) {
				this.successor2Count += (count ? 1 : -1);
				updatedCount = this.successor2Count;
				if (this.successor2Count == 0) {
					shift2();
				}
			}
			else if (this.successor3Index == NONE) {
				if (!count) System.err.println("Attempt to remove unseen event");
				this.successor3Index = key;
				this.successor3Count = 1;
				updatedCount = this.successor3Count;
			}
			else if (this.successor3Index == key) {
				this.successor3Count += (count ? 1 : -1);
				updatedCount = this.successor3Count;
				if (this.successor3Count == 0) {
					shift1();
				}
			}
			else {
				return false;
			}
			// If any update was successful, update the n-counts too
			this.updateNCounts(indices.size(), updatedCount, count);
		}
		if (fast || index == indices.size()) {
			this.updateCount(count);
			this.updateNCounts(index, this.getCount(), count);
		}
		return true;
	}

	private void shift3() {
		this.successor1Index = this.successor2Index;
		this.successor1Count = this.successor2Count;
		shift2();
	}

	private void shift2() {
		this.successor2Index = this.successor3Index;
		this.successor2Count = this.successor3Count;
		shift1();
	}

	private void shift1() {
		this.successor3Count = 0;
		this.successor3Index = NONE;
	}

	@Override
	protected int[] getShortCounts(List<Integer> sequence, int index) {
		int[] counts = new int[2];
		if (index == sequence.size() - 1) {
			counts[1] = this.count;
			Integer next = sequence.get(index);
			if (this.successor1Index == NONE) return counts;
			if (this.successor1Index == next) {
				counts[0] = this.successor1Count;
			}
			else if (this.successor2Index == NONE) return counts;
			else if (this.successor2Index == next) {
				counts[0] = this.successor2Count;
			}
			else if (this.successor3Index == NONE) return counts;
			else if (this.successor3Index == next) {
				counts[0] = this.successor3Count;
			}
			return counts;
		}
		else {
			return counts;
		}
	}

	@Override
	protected int[] getDistinctCounts(int range, List<Integer> sequence, int index) {
		int[] distinctCounts = new int[range];
		if (index == sequence.size()) {
			if (this.successor1Index != NONE) {
				distinctCounts[Math.min(range, this.successor1Count) - 1]++;
				if (this.successor2Index != NONE) {
					distinctCounts[Math.min(range, this.successor2Count) - 1]++;
					if (this.successor3Index != NONE) {
						distinctCounts[Math.min(range, this.successor3Count) - 1]++;
					}
				}
			}
		}
		return distinctCounts;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

}
