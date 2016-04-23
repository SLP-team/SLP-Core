package slp.core.counting.beta;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

public class BetaCounterSingle extends BetaCounter {

	public static final int NONE = (int) -1E10;
	protected BetaCounter successor1;
	protected int successor1Index = NONE;
	
	public BetaCounterSingle() {
		super();
	}
	
	@Override
	public int getDistinctSuccessors() {
		return this.successor1Index == NONE ? 0 : 1;
	}

	@Override
	public boolean update(List<Integer> indices, int index, boolean count, boolean fast) {
		// Cannot accommodate successors two steps ahead
		if (index < indices.size()) {
			Integer key = indices.get(index);
			if (this.successor1Index == NONE) {
				this.successor1Index = key;
				this.successor1 = new BetaCounterSingle();
				this.successor1.update(indices, index + 1, count, fast);
			}
			else if (this.successor1Index == key) {
				boolean success = this.successor1.update(indices, index + 1, count, fast);
				if (!success) return false;
				if (this.successor1.count == 0) {
					this.successor1Index = NONE;
					this.successor1 = null;
				}
			}
			else {
				return false;
			}
		}
		if (fast || index == indices.size()) {
			this.updateCount(count);
			this.updateNCounts(index, this.getCount(), count);
		}
		return true;
	}
	
	@Override
	protected int[] getShortCounts(List<Integer> sequence, int index) {
		int[] counts = new int[2];
		Integer key = sequence.get(index);
		BetaCounter successor = getSuccessor(key);
		if (index == sequence.size() - 1) {
			counts[1] = this.count;
			if (successor != null) counts[0] = successor.getCount();
			return counts;
		}
		else if (successor != null) {
			return successor.getShortCounts(sequence, index + 1);
		}
		else {
			return counts;
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
		}
		else {
			int[] distinctCounts = new int[range];
			if (this.successor1Index != NONE) {
				distinctCounts[Math.min(range, this.successor1.getCount()) - 1]++;
			}
			return distinctCounts;
		}
	}

	private BetaCounter getSuccessor(Integer key) {
		if (this.successor1Index == NONE) {
			return null;
		}
		else if (this.successor1Index == key) {
			return this.successor1;
		}
		return null;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(3);
		out.writeInt(this.count);
		if (this.successor1 != null) {
			out.writeInt(1);
			out.writeInt(this.successor1Index);
			this.successor1.writeExternal(out);	
		}
		else {
			out.writeInt(0);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.count = in.readInt();
		BetaCounter.nCounts[depthForReadingIn - 1][Math.min(this.count, 4) - 1]++;
		int successors = in.readInt();
		if (successors > 0) {
			this.successor1Index = in.readInt();
			int type1 = in.readInt();
			BetaCounter counter;
			switch (type1) {
				case 0: counter = new BetaCounterMap(); break;
				case 1: counter = new BetaCounterArray(); break;
				case 2: counter = new BetaCounterSmall(); break;
				case 3: counter = new BetaCounterSingle(); break;
				default: counter = new BetaCounterMap();
			}
			BetaCounter.depthForReadingIn++;
			counter.readExternal(in);
			BetaCounter.depthForReadingIn--;
			this.successor1 = counter;
		}
	}

}
