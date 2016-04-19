package slp.core.counting.beta;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.util.Configuration;

public abstract class BetaCounter implements Counter {

	protected int count;
	protected static int[][] nCounts = new int[Configuration.order()][4];
	
	public BetaCounter() {
		this.count = 0;
	}
	
	@Override
	public int getCount() {
		return this.count;
	}
	
	public void updateNCounts(int n, int count, boolean added) {
		if (n == 0) return;
		if (count > 0 && count < 5) {
			nCounts[n - 1][count - 1]++;
		}
		int prevCount = added ? count - 1 : count + 1;
		if (prevCount > 0 && prevCount < 5) {
			nCounts[n - 1][prevCount - 1]--;
		}
	}
	
	public int getNCount(int n, int count) {
		return nCounts[n - 1][count - 1];
	}
	
	public void updateCount(boolean count) {
		if (count) inc(); else dec();
	}

	private void inc() {
		this.count++;
	}
	
	private void dec() {
		this.count--;
	}

	@Override
	public final void update(List<Integer> indices, boolean count) {
		update(indices, 0, count, false);
	}
	
	@Override
	public final void updateForward(List<Integer> indices, boolean count) {
		update(indices, 0, count, true);
	}
	
	public abstract boolean update(List<Integer> indices, int index, boolean count, boolean fast);
	
	@Override
	public int[] getShortCounts(Stream<Integer> indices) {
		return getShortCounts(indices.collect(Collectors.toList()), 0);
	}

	protected abstract int[] getShortCounts(List<Integer> sequence, int index);

	@Override
	public int[] getDistinctCounts(int range, Stream<Integer> indices) {
		return getDistinctCounts(range, indices.collect(Collectors.toList()), 0);
	}
	
	protected abstract int[] getDistinctCounts(int range, List<Integer> sequence, int index);
}
