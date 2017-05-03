package slp.core.sequencing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.modeling.ModelRunner;

public class NGramSequencer {

	public static List<List<Integer>> sequenceForward(Stream<Integer> tokens) {
		return sequenceForward(tokens.collect(Collectors.toList()));
	}
	public static List<List<Integer>> sequenceForward(List<Integer> tokens) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int start = 0; start < tokens.size(); start++) {
			int end = Math.min(tokens.size(), start + ModelRunner.getNGramOrder());
			result.add(tokens.subList(start, end));
		}
		return result;
	}

	public static List<List<Integer>> sequenceBackward(Stream<Integer> tokens) {
		return sequenceBackward(tokens.collect(Collectors.toList()));
	}
	public static List<List<Integer>> sequenceBackward(List<Integer> tokens) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int end = 1; end <= tokens.size(); end++) {
			int start = Math.max(0, end - ModelRunner.getNGramOrder());
			result.add(tokens.subList(start, end));
		}
		return result;
	}
	
	public static List<Integer> sequenceAt(List<Integer> tokens, int index) {
		return tokens.subList(Math.max(0, index - ModelRunner.getNGramOrder() + 1), index + 1);
	}
}
