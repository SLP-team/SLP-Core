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
	public abstract int[] getShortCounts(Stream<Integer> indices);

	public abstract int getDistinctSuccessors();
	
	public abstract int[] getDistinctCounts(int range, Stream<Integer> indices);

	public abstract int getNCount(int n, int count);

	public void update(List<Integer> indexStream, boolean count);
	
	public default void update(Stream<Integer> indexStream, boolean count) {
		update(indexStream.collect(Collectors.toList()), count);
	}

	/*
	 * Convenience methods for retrieving counts
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
	public default int[][] getFullCounts(Stream<Integer> indices) {
		List<Integer> sequence = indices.collect(Collectors.toList());
		return getFullCounts(sequence);
	}

	public default int[][] getFullCounts(List<Integer> sequence) {
		int[][] counts = new int[2][sequence.size()];
		for (int i = 0; i < sequence.size(); i++) {
			int[] shortCounts = getShortCounts(sequence.subList(i, sequence.size()).stream());
			int index = sequence.size() - i - 1;
			counts[0][index] = shortCounts[0];
			counts[1][index] = shortCounts[1];
		}
		return counts;
	}

	public default int[][] getFullCounts(Stream<Integer> indices, int startIndex) {
		return getFullCounts(indices.skip(startIndex));
	}

	public default int[] getShortCounts(Stream<Integer> indices, int startIndex) {
		return getShortCounts(indices.skip(startIndex));
	}

	public default int[] getDistinctCounts(int range, Stream<Integer> indices, int startIndex) {
		return getDistinctCounts(range, indices.skip(startIndex));
	}

	/*
	 * Convenience methods for updating counts, possibly yielding superior performance in sub-classes
	 */
	public default void update(Stream<Integer> indexStream, boolean count, int startIndex) {
		update(indexStream.skip(startIndex), count);
	}

	public default void add(Stream<Integer> indexStream) {
		update(indexStream, true, 0);
	}

	public default void remove(Stream<Integer> indexStream) {
		update(indexStream, false, 0);
	}

	/**
	 * Update the counter with all (sub-)sequences that contain the last element in the provided sequence (and the empty sequence).
	 */
	public default void updateBackward(Stream<Integer> indexStream, boolean count) {
		List<Integer> indices = indexStream.collect(Collectors.toList());
		updateBackward(indices, count);
	}

	public default void updateBackward(List<Integer> indices, boolean count) {
		for (int i = 0; i <= indices.size(); i++) {
			update(indices.subList(i, indices.size()), count);
		}
	}
	
	public default void updateBackward(Stream<Integer> indexStream, boolean count, int startIndex) {
		updateBackward(indexStream.skip(startIndex), count);
	}

	public default void addBackwards(Stream<Integer> indexStream) {
		updateBackward(indexStream, true, 0);
	}

	public default void removeBackward(Stream<Integer> indexStream) {
		updateBackward(indexStream, false, 0);
	}

	public default void updateForward(Stream<Integer> indexStream, boolean count) {
		List<Integer> indices = indexStream.collect(Collectors.toList());
		updateForward(indices, count);
	}

	public default void updateForward(List<Integer> indices, boolean count) {
		for (int i = 0; i < indices.size(); i++) {
			update(indices.subList(0, i + 1), count);
		}
	}

	public default void updateForward(Stream<Integer> indexStream, boolean count, int startIndex) {
		updateForward(indexStream.skip(startIndex), count);
	}
		
	public default void addForward(Stream<Integer> indexStream) {
		updateForward(indexStream, true);
	}

	public default void addForward(List<Integer> indexStream) {
		updateForward(indexStream, true);
	}
	
	public default void removeForward(Stream<Integer> indexStream) {
		updateForward(indexStream, false);
	}
	
	public default void removeForward(List<Integer> indexStream) {
		updateForward(indexStream, true);
	}
}
