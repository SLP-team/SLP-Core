package slp.core.sequencing;

import java.util.ArrayList;
import java.util.List;

import slp.core.modeling.ModelRunner;

/**
 * Work in progress, no guarantees of functionality yet.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class SkipGramSequencer {

	public static List<List<Integer>> sequenceBackward(List<Integer> tokens) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int start = 0; start < tokens.size() - 1; start++) {
			for (int end = start + 1; end <= start + ModelRunner.getNGramOrder() && end < tokens.size(); end++) {
				List<Integer> gram = new ArrayList<Integer>();
				gram.add(tokens.get(start));
				gram.add(tokens.get(end));
				result.add(gram);
			}
		}
		return result;
	}
}
