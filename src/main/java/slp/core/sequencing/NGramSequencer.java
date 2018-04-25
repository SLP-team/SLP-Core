package slp.core.sequencing;

import java.util.ArrayList;
import java.util.List;

import slp.core.modeling.runners.ModelRunner;

public class NGramSequencer {

	public static List<List<Integer>> sequenceForward(List<Integer> tokens, int maxOrder) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int start = 0; start < tokens.size(); start++) {
			int end = Math.min(tokens.size(), start + maxOrder);
			result.add(tokens.subList(start, end));
		}
		return result;
	}

	public static List<List<Integer>> sequenceAround(List<Integer> tokens, int index, int maxOrder) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		int firstLoc = index - maxOrder + 1;
		for (int start = Math.max(0, firstLoc); start <= index; start++) {
			int end = Math.min(tokens.size(), start + maxOrder);
			result.add(tokens.subList(start, end));
		}
		return result;
	}
	
	/**
	 * Returns the longest possible sublist that doesn't exceed {@linkplain ModelRunner#getNGramOrder()} in length
	 * and <u>includes</u> the token at index {@code index}.
	 */
	public static List<Integer> sequenceAt(List<Integer> tokens, int index, int maxOrder) {
		return tokens.subList(Math.max(0, index - maxOrder + 1), index + 1);
	}
}
