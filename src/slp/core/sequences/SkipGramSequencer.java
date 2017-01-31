package slp.core.sequences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.Vocabulary;
import slp.core.util.Configuration;

public class SkipGramSequencer implements Sequencer {

	private final Vocabulary vocabulary;
	
	public SkipGramSequencer() {
		this(new Vocabulary());
	}

	public SkipGramSequencer(Vocabulary vocabulary) {
		this.vocabulary = vocabulary;
	}

	@Override
	public Stream<List<Integer>> sequenceForward(Stream<Integer> in) {
		return sequenceBackward(in);
	}
	
	@Override
	public Stream<List<Integer>> sequenceBackward(Stream<Integer> in) {
		List<Integer> line = in.collect(Collectors.toList());
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int start = 0; start < line.size() - 1; start++) {
			for (int end = start + 1; end <= start + Configuration.order() && end < line.size(); end++) {
				List<Integer> gram = new ArrayList<Integer>();
				gram.add(line.get(start));
				gram.add(line.get(end));
				result.add(gram);
			}
		}
		return result.stream();
	}

	public Vocabulary getVocabulary() {
		return this.vocabulary;
	}
}
