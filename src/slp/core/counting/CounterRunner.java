package slp.core.counting;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import slp.core.io.Reader;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;

public class CounterRunner {

	public static void count(File file, Counter counter, Tokenizer tokenizer, Vocabulary vocabulary) {
		count(file, counter, tokenizer, vocabulary, Sequencer.standard());
	}
	
	public static void count(File file, Counter counter, Tokenizer tokenizer, Vocabulary vocabulary, boolean add) {
		count(file, counter, tokenizer, vocabulary, Sequencer.standard(), add);
	}

	public static void count(File file, Counter counter, Tokenizer tokenizer, Vocabulary vocabulary, Sequencer sequencer) {
		count(file, counter, tokenizer, vocabulary, sequencer, true);
	}
	
	public static void count(File file, Counter counter, Tokenizer tokenizer, Vocabulary vocabulary, Sequencer sequencer, boolean add) {
		Stream<List<Integer>> sequences = Stream.of(Reader.readContent(file))
				.map(tokenizer::tokenize)
				.map(vocabulary::toIndices)
				.flatMap(sequencer::sequenceForward);
		if (add) sequences.forEachOrdered(counter::addForward);
		else sequences.forEachOrdered(counter::removeForward);		
	}
}
