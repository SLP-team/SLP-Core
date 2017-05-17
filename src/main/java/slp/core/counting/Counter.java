package slp.core.counting;

import java.io.Externalizable;
import java.util.List;

import slp.core.counting.trie.TrieCounter;
import slp.core.modeling.ngram.NGramModel;

/**
 * Interface for counter implementations that can be used by count-based models,
 * most notoriously the {@link TrieCounter} which provides a rather efficient implementation
 * that is currently used by the {@link NGramModel}s.
 * 
 * @author Vincent Hellendoorn
 *
 */
public interface Counter extends Externalizable {
	
	/**
	 * Convenience method, returns count of Counter object (e.g. root node in trie-counter)
	 * 
	 * @return count of this Counter
	 */
	public int getCount();
	
	/**
	 * Returns [context-count, count] pair of {@code indices}, for convenient MLE.
	 * Note: poorly defined on empty list.
	 * 
	 * @param indices Sequence of stored, translated tokens to return counts for
	 * @return The stored [context-count, count] pair of indices
	 */
	public long[] getCounts(List<Integer> indices);

	/**
	 * Returns the number of sequences of length n seen `count' times
	 */
	public abstract int getCountofCount(int n, int count);
	
	public int getSuccessorCount();
	public int getSuccessorCount(List<Integer> indices);
	public List<Integer> getTopSuccessors(List<Integer> indices, int limit);
	
	public int[] getDistinctCounts(int range, List<Integer> indices);

	public void count(List<Integer> indices);
	public void unCount(List<Integer> indices);

	public default void countBatch(List<List<Integer>> indices) {
		indices.forEach(this::count);
	}
	public default void unCountBatch(List<List<Integer>> indices) {
		indices.forEach(this::unCount);
	}
}
