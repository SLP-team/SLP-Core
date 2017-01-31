package slp.core.modeling;

import java.io.File;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import slp.core.counting.Vocabulary;
import slp.core.io.Reader;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;

public class ModelRunner {

	private static final double log2 = Math.log(2);

	public static DoubleSummaryStatistics model(File file, Model model, Vocabulary vocabulary) {
		return model(file, model, Tokenizer.standard(), vocabulary);
	}
	
	public static DoubleSummaryStatistics model(File file, Model model, Tokenizer tokenizer, Vocabulary vocabulary) {
		return model(file, model, tokenizer, vocabulary, Sequencer.standard());
	}
	
	public static DoubleSummaryStatistics model(File file, Model model, Tokenizer tokenizer, Vocabulary vocabulary, Sequencer sequencer) {
		return Stream.of(Reader.readContent(file))
			.map(tokenizer::tokenize)
			.map(vocabulary::toIndices)
			.flatMap(sequencer::sequenceBackward)
			.mapToDouble(model::model)
			.map(x -> -Math.log(x)/log2)
			.summaryStatistics();
	}

	public static int[] predict(File file, int limit, Model model, Vocabulary vocabulary) {
		return predict(file, limit, model, Tokenizer.standard(), vocabulary);
	}
	
	public static int[] predict(File file, int limit, Model model, Tokenizer tokenizer, Vocabulary vocabulary) {
		return predict(file, limit, model, tokenizer, vocabulary, Sequencer.standard());
	}
	
	public static int[] predict(File file, int limit, Model model, Tokenizer tokenizer, Vocabulary vocabulary, Sequencer sequencer) {
		int[] ranks = new int[limit + 1];
		Stream.of(Reader.readContent(file))
				.map(tokenizer::tokenize)
				.map(vocabulary::toIndices)
				.flatMap(sequencer::sequenceBackward)
				.mapToInt(l -> getRank(model, l, limit))
				.forEach(r -> ranks[r]++);
		return ranks;
	}
	
	public static DoubleSummaryStatistics getMRR(int[] rankCounts) {
		DoubleSummaryStatistics stats = IntStream.range(0, rankCounts.length)
			.mapToObj(i -> IntStream.range(0, rankCounts[i]).mapToDouble(j -> i > 0 ? 1.0 / i : 0.0))
			.flatMapToDouble(x -> x)
			.summaryStatistics();
		return stats;
	}
	
	private static int getRank(Model model, List<Integer> in, int limit) {
		if (in.size() <= 1) return 0;
		List<Integer> ranked = model.predict(new ArrayList<>(in.subList(0, in.size() - 1)), limit);
		return ranked.indexOf(in.get(in.size() - 1)) + 1;
	}
}
