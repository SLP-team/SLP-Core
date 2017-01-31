package slp.core.sequences;

import java.util.List;
import java.util.stream.Stream;

public interface Sequencer {

	/* 
	 * There are two ways to extract sequences out of a line, potentially enabling faster counting:
	 * 1. Forward: each longest sequence (Configuration->ORDER, or the whole sentence once if it's shorter) is returned once.
	 *    - If sentence shorter than Configuration's ORDER, whole sentence is returned once.
	 *    - Useful for counting sequential data in a forward trie, since counting at every node (i.e. step of the sequence) 
	 *      immediately counts subsequences as well.
	 * 2. Backward: each element after the start-of-sentence marker (presumed present) is returned once in its longest context.
	 *    - This is used for modeling sequential data since we need a probability per token
	 *    - This yield the same benefit for a reverse trie as the Forward method does for a regular trie.
	 */
	public Stream<List<Integer>> sequenceForward(Stream<Integer> in);
	public Stream<List<Integer>> sequenceBackward(Stream<Integer> in);

	public static Sequencer standard() {
		return new NGramSequencer();
	}
}
