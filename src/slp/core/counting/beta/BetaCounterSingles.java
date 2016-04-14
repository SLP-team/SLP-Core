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
	public boolean update(List<Integer> indices, int index, boolean count) {
		// Cannot accommodate successors two steps ahead
		if (index < indices.size() - 1) return false;
		else if (index < indices.size()) {
			if (this.successor1Index == index) {
				this.successor1Count += (count ? 1 : - 1);
				if (this.successor1Count == 0) {
					this.successor1Index = NONE;
				}
			}
			else if (this.successor2Index == index) {
				this.successor2Count += (count ? 1 : -1);
				if (this.successor2Count == 0) {
					this.successor2Index = NONE;
				}
			}
			else if (this.successor3Index == index) {
				this.successor3Count += (count ? 1 : -1);
				if (this.successor3Count == 0) {
					this.successor3Index = NONE;
				}
			}
			else {
				return false;
			}
		}
		else {
			this.updateCount(count);
		}
		return true;
	}

	@Override
	protected BetaCounter getOrCreateSuccessor(Integer first) {
		return null;
	}

	@Override
	protected BetaCounter getSuccessor(Integer first) {
		return null;
	}

	@Override
	protected int[] getShortCounts(List<Integer> sequence, int index) {
		int[] counts = new int[2];
		if (index == sequence.size() - 1) {
			counts[0] = this.count;
			Integer next = sequence.get(index);
			if (this.successor1Index == next) {
				counts[1] = this.successor1Count;
			}
			else if (this.successor2Index == next) {
				counts[1] = this.successor2Count;
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
		if (index == sequence.size() - 1) {
			int dis1 = Math.min(this.successor1Count, range - 1);
			if (dis1 > 0) {
				distinctCounts[dis1]++;
			}
			int dis2 = Math.min(this.successor2Count, range - 1);
			if (dis2 > 0) {
				distinctCounts[dis2]++;
			}
			return distinctCounts;
		}
		else {
			return distinctCounts;
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

}
