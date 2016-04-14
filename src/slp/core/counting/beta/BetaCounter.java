package slp.core.counting.beta;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.Counter;

public abstract class BetaCounter implements Counter {

	protected int count;
	
	public BetaCounter() {
		this.count = 0;
	}
	
	@Override
	public int getCount() {
		return this.count;
	}
	
	public synchronized void updateCount(boolean count) {
		if (count) inc(); else dec();
	}

	private void inc() {
		this.count++;
	}
	
	private void dec() {
		this.count--;
	}


	@Override
	public final void update(Stream<Integer> indexStream, boolean count) {
		update(indexStream.collect(Collectors.toList()), 0, count);
	}
	
	public abstract boolean update(List<Integer> indexStream, int index, boolean count);
	
	protected abstract BetaCounter getOrCreateSuccessor(Integer first);

	protected abstract BetaCounter getSuccessor(Integer first);
	
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
