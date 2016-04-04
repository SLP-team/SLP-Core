package slp.core.counting;

import java.io.Externalizable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Counter extends Externalizable {
	
	public static Counter standard() {
		return new TrieGramCounter();
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

	public void update(Stream<Integer> indexStream, boolean count, int startIndex);

	public int[][] getFullCounts(Stream<Integer> indices, int startIndex);

	public int[] getShortCounts(Stream<Integer> indices, int startIndex);

	public int[] getDistinctCounts(int range, Stream<Integer> indices, int startIndex);

	/*
	 * Alternative update methods that may lead to faster counting, depending on the counter implementation
	 */
	/**
	 * Update the counter with all (sub-)sequences that contain the last element in the provided sequence.
	 * @param indexStream
	 * @param count
	 * @param startIndex
	 */
	public default void updateBackward(Stream<Integer> indexStream, boolean count, int startIndex) {
		List<Integer> indices = indexStream.collect(Collectors.toList());
		for (int i = 0; i < indices.size(); i++) {
			update(indices.subList(i, indices.size()).stream(), count, startIndex);
		}
	}
	
	/**
	 * Update the counter with all (sub-)sequences that contain the first element in the provided sequence.
	 * @param indexStream
	 * @param count
	 * @param startIndex
	 */
	public default void updateForward(Stream<Integer> indexStream, boolean count, int startIndex) {
		List<Integer> indices = indexStream.collect(Collectors.toList());
		for (int i = 0; i < indices.size(); i++) {
			update(indices.subList(0, i + 1).stream(), count, startIndex);
		}
	}
		
	/*
	 * Convenience methods that don't require start/end indices to be specified &
	 * convenience add/remove methods.
	 */
	public default void update(Stream<Integer> indexStream, boolean count) {
		update(indexStream, count, 0);
	}
	public default void add(Stream<Integer> indexStream) {
		update(indexStream, true, 0);
	}
	public default void remove(Stream<Integer> indexStream) {
		update(indexStream, false, 0);
	}
	
	public default void updateBackward(Stream<Integer> indexStream, boolean count) {
		updateBackward(indexStream, count, 0);
	}
	public default void addBackwards(Stream<Integer> indexStream) {
		updateBackward(indexStream, true, 0);
	}
	public default void removeBackward(Stream<Integer> indexStream) {
		updateBackward(indexStream, false, 0);
	}
	
	public default void updateForward(Stream<Integer> indexStream, boolean count) {
		updateForward(indexStream, count, 0);
	}
	public default void addForward(Stream<Integer> indexStream) {
		updateForward(indexStream, true, 0);
	}
	public default void removeForward(Stream<Integer> indexStream) {
		updateForward(indexStream, false, 0);
	}
	
	public default int[][] getFullCounts(Stream<Integer> indices) {
		return getFullCounts(indices, 0);
	}
	public default int[] getShortCounts(Stream<Integer> indices) {
		return getShortCounts(indices, 0);
	}
	public default int[] getDistinctCounts(int range, Stream<Integer> indices) {
		return getDistinctCounts(range, indices, 0);
	}
}
