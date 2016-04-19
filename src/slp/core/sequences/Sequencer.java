package slp.core.sequences;

import java.util.List;
import java.util.stream.Stream;

public interface Sequencer {

	/*
	 * Two methods for extracting continuous sequences out of a line, which may greatly improve counting/modeling performance.
	 * 
	 * There are two ways to extract sequences out of a line that may enable faster counting:
	 * 1. Forwards: each element is returned with as long a context as is available.
	 * 2. Backwards: for each element, exactly one sequence is returned in which it is terminal symbol.
	 * 
	 * 1. is great for counting fast in e.g. tries; 2. is useful for modeling
	 * By default, these methods simply invoke "sequence"
	 */
	public Stream<List<Integer>> sequenceForward(Stream<Integer> in);
	public Stream<List<Integer>> sequenceBackward(Stream<Integer> in);

	public static Sequencer standard() {
		return new NGramSequencer();
	}
}
