package slp.core.sequences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.Vocabulary;
import slp.core.util.Configuration;

public class NGramSequencer implements Sequencer {

	private final Vocabulary vocabulary;

	public NGramSequencer() {
		this(new Vocabulary());
	}

	public NGramSequencer(Vocabulary vocabulary) {
		this.vocabulary = vocabulary;
	}

	@Override
	public Stream<List<Integer>> sequenceForward(Stream<Integer> in) {
		List<Integer> line = in.collect(Collectors.toList());
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int start = 0; start < line.size(); start++) {
			int end = Math.min(line.size(), start + Configuration.order());
			result.add(line.subList(start, end));
		}
		return result.stream();
	}
	
	@Override
	public Stream<List<Integer>> sequenceBackward(Stream<Integer> in) {
		List<Integer> line = in.collect(Collectors.toList());
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int end = 1; end <= line.size(); end++) {
			int start = Math.max(0, end - Configuration.order());
			result.add(line.subList(start, end));
		}
		return result.stream();
	}

	public Vocabulary getVocabulary() {
		return this.vocabulary;
	}
}
