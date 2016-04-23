package slp.core.counting.beta;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

public class BetaCounterSmall extends BetaCounter {

	public static final int NONE = (int) -1E10;
	protected BetaCounter successor1;
	protected BetaCounter successor2;
	protected BetaCounter successor3;
	protected int successor1Index = NONE;
	protected int successor2Index = NONE;
	protected int successor3Index = NONE;
	
	public BetaCounterSmall() {
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
		if (index < indices.size()) {
			Integer key = indices.get(index);
			if (this.successor1Index == NONE) {
				this.successor1Index = key;
				this.successor1 = new BetaCounterSingle();
				this.successor1.update(indices, index + 1, count, fast);
			}
			else if (this.successor1Index == key) {
				boolean success = this.successor1.update(indices, index + 1, count, fast);
				if (!success) {
					BetaCounter newSuccessor = promote(key);
					// cannot promote successor past Array
					if (newSuccessor == null) return false;
					this.successor1 = newSuccessor;
					return update(indices, index, count, fast);
				}
				if (this.successor1.count == 0) {
					shift3();
				}
			}
			else if (this.successor2Index == NONE) {
				this.successor2Index = key;
				this.successor2 = new BetaCounterSingle();
				this.successor2.update(indices, index + 1, count, fast);
			}
			else if (this.successor2Index == key) {
				boolean success = this.successor2.update(indices, index + 1, count, fast);
				if (!success) {
					BetaCounter newSuccessor = promote(key);
					// cannot promote successor past Array
					if (newSuccessor == null) return false;
					this.successor2 = newSuccessor;
					return update(indices, index, count, fast);
				}
				if (this.successor2.count == 0) {
					shift2();
				}
			}
			else if (this.successor3Index == NONE) {
				this.successor3Index = key;
				this.successor3 = new BetaCounterSingle();
				this.successor3.update(indices, index + 1, count, fast);
			}
			else if (this.successor3Index == key) {
				boolean success = this.successor3.update(indices, index + 1, count, fast);
				if (!success) {
					BetaCounter newSuccessor = promote(key);
					// cannot promote successor past Array
					if (newSuccessor == null) return false;
					this.successor3 = newSuccessor;
					return update(indices, index, count, fast);
				}
				if (this.successor3.count == 0) {
					shift1();
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

	private void shift3() {
		this.successor1Index = this.successor2Index;
		this.successor1 = this.successor2;
		shift2();
	}

	private void shift2() {
		this.successor2Index = this.successor3Index;
		this.successor2 = this.successor3;
		shift1();
	}

	private void shift1() {
		this.successor3 = null;
		this.successor3Index = NONE;
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
				if (this.successor2Index != NONE) {
					distinctCounts[Math.min(range, this.successor2.getCount()) - 1]++;
					if (this.successor3Index != NONE) {
						distinctCounts[Math.min(range, this.successor3.getCount()) - 1]++;
					}
				}
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
		else if (this.successor2Index == NONE) {
			return null;
		}
		else if (this.successor2Index == key) {
			return this.successor2;
		}
		else if (this.successor3Index == NONE) {
			return null;
		}
		else if (this.successor3Index == key) {
			return this.successor3;
		}
		return null;
	}
	
	/*
	 * A small counter promotes its successors from single to small
	 */
	private BetaCounter promote(Integer key) {
		BetaCounter curr = getSuccessor(key);
		if (curr instanceof BetaCounterSingle) {
			return promoteSingleToSmall(key, (BetaCounterSingle) curr);
		} 
		else if (curr instanceof BetaCounterSmall) {
			return null;
		}
		return curr;
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
		out.writeInt(2);
		out.writeInt(this.count);
		int successors = getDistinctSuccessors();
		out.writeInt(successors);
		if (successors > 0) {
			out.writeInt(this.successor1Index);
			this.successor1.writeExternal(out);
			if (successors > 1) {
				out.writeInt(this.successor2Index);
				this.successor2.writeExternal(out);
				if (successors > 2) {
					out.writeInt(this.successor3Index);
					this.successor3.writeExternal(out);
				}
			}
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
			BetaCounter counter1;
			switch (type1) {
				case 0: counter1 = new BetaCounterMap(); break;
				case 1: counter1 = new BetaCounterArray(); break;
				case 2: counter1 = new BetaCounterSmall(); break;
				case 3: counter1 = new BetaCounterSingle(); break;
				default: counter1 = new BetaCounterMap();
			}
			BetaCounter.depthForReadingIn++;
			counter1.readExternal(in);
			BetaCounter.depthForReadingIn--;
			this.successor1 = counter1;
			if (successors > 1) {
				this.successor2Index = in.readInt();
				int type2 = in.readInt();
				BetaCounter counter2;
				switch (type2) {
					case 0: counter2 = new BetaCounterMap(); break;
					case 1: counter2 = new BetaCounterArray(); break;
					case 2: counter2 = new BetaCounterSmall(); break;
					case 3: counter2 = new BetaCounterSingle(); break;
					default: counter2 = new BetaCounterMap();
				}
				BetaCounter.depthForReadingIn++;
				counter2.readExternal(in);
				BetaCounter.depthForReadingIn--;
				this.successor2 = counter2;
				if (successors > 2) {
					this.successor3Index = in.readInt();
					int type3 = in.readInt();
					BetaCounter counter3;
					switch (type3) {
						case 0: counter3 = new BetaCounterMap(); break;
						case 1: counter3 = new BetaCounterArray(); break;
						case 2: counter3 = new BetaCounterSmall(); break;
						case 3: counter3 = new BetaCounterSingle(); break;
						default: counter3 = new BetaCounterMap();
					}
					BetaCounter.depthForReadingIn++;
					counter3.readExternal(in);
					BetaCounter.depthForReadingIn--;
					this.successor3 = counter3;
				}
			}
		}
	}

}
