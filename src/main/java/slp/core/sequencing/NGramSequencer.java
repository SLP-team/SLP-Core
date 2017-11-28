package slp.core.sequencing;

import java.util.ArrayList;
import java.util.List;

import slp.core.modeling.ModelRunner;

public class NGramSequencer {

	public static List<List<Integer>> sequenceForward(List<Integer> tokens) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int start = 0; start < tokens.size(); start++) {
			int end = Math.min(tokens.size(), start + ModelRunner.getNGramOrder());
			result.add(tokens.subList(start, end));
		}
		return result;
	}

	public static List<List<Integer>> sequenceAround(List<Integer> tokens, int index) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		int firstLoc = index - ModelRunner.getNGramOrder() + 1;
		for (int start = Math.max(0, firstLoc); start <= index; start++) {
			int end = Math.min(tokens.size(), start + ModelRunner.getNGramOrder());
			result.add(tokens.subList(start, end));
		}
		return result;
	}
	
	public static List<Integer> sequenceAt(List<Integer> tokens, int index) {
		return tokens.subList(Math.max(0, index - ModelRunner.getNGramOrder() + 1), index + 1);
	}
}
