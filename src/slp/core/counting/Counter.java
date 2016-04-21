package slp.core.counting;

import java.io.Externalizable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.beta.BetaCounterMap;

public interface Counter extends Externalizable {
	
	public static Counter standard() {
		return new BetaCounterMap();
	}

	/*
	 * Primary interface methods here.
	 */
	public abstract int getCount();

	/**
	 * Return a [count, context-count] pair for this sequence of indices
	 * @param indices
	 * @return
	 */
	public abstract int[] getShortCounts(List<Integer> indices);

	public abstract int getDistinctSuccessors();

	public abstract int[] getDistinctCounts(int range, List<Integer> indices);

	public abstract int getNCount(int n, int count);

	public abstract void update(List<Integer> indexStream, boolean count);
	
	public default void update(Stream<Integer> indexStream, boolean count) {
		update(indexStream.collect(Collectors.toList()), count);
	}

	/*
	 * Convenience methods below
	 */
	/**
	 * Returns the full n-gram counts of a sequence at once, possibly utilizing faster lookup for context/count pairs.
	 * For instance:
	 * full counts of [a, b, c] are:
	 *  [c(c), c(b, c), c(a, b, c)]
	 *  [c(*), c(b),    c(a, b)]
	 * @param indices
	 * @return
	 */
	public default int[][] getFullCounts(List<Integer> sequence) {
		int[][] counts = new int[2][sequence.size()];
		for (int i = 0; i < sequence.size(); i++) {
			int[] shortCounts = getShortCounts(sequence.subList(i, sequence.size()));
			int index = sequence.size() - i - 1;
			counts[0][index] = shortCounts[0];
			counts[1][index] = shortCounts[1];
		}
		return counts;
	}

	/**
	 * Update the counter with all (sub-)sequences that contain the last element in the provided sequence (and the empty sequence).
	 */
	public default void updateBackward(List<Integer> indices, boolean count) {
		for (int i = 0; i <= indices.size(); i++) {
			update(indices.subList(i, indices.size()), count);
		}
	}

	public default void addBackward(List<Integer> indexStream) {
		updateBackward(indexStream, true);
	}

	public default void removeBackward(List<Integer> indexStream) {
		updateBackward(indexStream, false);
	}

	public default void updateForward(List<Integer> indices, boolean count) {
		for (int i = 0; i < indices.size(); i++) {
			update(indices.subList(0, i + 1), count);
		}	
	}

	public default void addForward(List<Integer> indices) {
		updateForward(indices, true);
	}
	
	public default void removeForward(List<Integer> indices) {
		updateForward(indices, false);
	}
}
