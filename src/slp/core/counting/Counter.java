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
	/**
	 * Update the counter with only the count of the full sequence provided.
	 * @param indexStream
	 * @param count
	 * @param startIndex
	 */
	public abstract int getCount();

	public abstract int getDistinctSuccessors();
	
	public abstract int getNCount(int n, int count);

	public void update(Stream<Integer> indexStream, boolean count);

	/**
	 * Return a [count, context-count] pair for this sequence of indices
	 * @param indices
	 * @return
	 */
	public int[] getShortCounts(Stream<Integer> indices);
	
	public int[] getDistinctCounts(int range, Stream<Integer> indices);
	
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
		int[][] counts = new int[2][sequence.size()];
		for (int i = 0; i < sequence.size(); i++) {
			int[] shortCounts = getShortCounts(sequence.subList(i, sequence.size()).stream());
			int index = sequence.size() - i - 1;
			counts[0][index] = shortCounts[0];
			counts[1][index] = shortCounts[1];
		}
		return counts;
	}

	/**
	 * Update the counter with all (sub-)sequences that contain the last element in the provided sequence (and the empty sequence).
	 */
	public default void updateBackward(Stream<Integer> indexStream, boolean count) {
		List<Integer> indices = indexStream.collect(Collectors.toList());
		for (int i = 0; i <= indices.size(); i++) {
			Stream<Integer> stream = indices.subList(i, indices.size()).stream();
			update(stream, count);
		}
	}
	
	/**
	 * Update the counter with all (sub-)sequences that contain the first element in the provided sequence.
	 */
	public default void updateForward(Stream<Integer> indexStream, boolean count) {
		List<Integer> indices = indexStream.collect(Collectors.toList());
		for (int i = 0; i < indices.size(); i++) {
			update(indices.subList(0, i + 1).stream(), count);
		}
	}

	/*
	 * Convenience methods that don't require start/end indices to be specified &
	 * convenience add/remove methods.
	 */
	public default void updateForward(Stream<Integer> indexStream, boolean count, int startIndex) {
		updateForward(indexStream.skip(startIndex), count);
	}
		
	public default void update(Stream<Integer> indexStream, boolean count, int startIndex) {
		update(indexStream.skip(startIndex), count);
	}

	public default void add(Stream<Integer> indexStream) {
		update(indexStream, true, 0);
	}
	public default void remove(Stream<Integer> indexStream) {
		update(indexStream, false, 0);
	}
	
	/*
	 * Alternative update methods that may lead to faster counting, depending on the counter implementation
	 */
	public default void updateBackward(Stream<Integer> indexStream, boolean count, int startIndex) {
		updateBackward(indexStream.skip(startIndex), count);
	}

	public default void addBackwards(Stream<Integer> indexStream) {
		updateBackward(indexStream, true, 0);
	}
	public default void removeBackward(Stream<Integer> indexStream) {
		updateBackward(indexStream, false, 0);
	}

	public default void addForward(Stream<Integer> indexStream) {
		updateForward(indexStream, true, 0);
	}
	public default void removeForward(Stream<Integer> indexStream) {
		updateForward(indexStream, false, 0);
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
}
