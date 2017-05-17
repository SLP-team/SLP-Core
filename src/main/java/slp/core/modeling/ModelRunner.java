package slp.core.modeling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import slp.core.lexing.LexerRunner;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;

/**
 * The ModelRunner class provides the third step in modeling (after lexing, translating).
 * It can be configured statically and exposes static modeling methods for convenience.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class ModelRunner {
	
	private static final double NEG_LOG_2 = -Math.log(2);
	private static boolean perLine = false;
	private static boolean selfTesting = false;
	
	private static int ngramOrder = 6;
	private static int predictionCutoff = 10;

	/**
	 * Treat each line separately (default: false).
	 * <br />
	 * <em>Note:</em> does not in any way interact with {@link LexerRunner#perLine()}!
	 * So if the lexer does not append line delimiters, this code won't do so either
	 * but will still run on each line separately.
	 * 
	 * @param perLine 
	 */
	public static void perLine(boolean perLine) {
		ModelRunner.perLine = perLine;
	}
	
	/**
	 * Returns whether or not this class will model each line in isolation.
	 */
	public static boolean isPerLine() {
		return ModelRunner.perLine;
	}
	
	/**
	 * Indicate that we are testing on the training set,
	 * which means we must 'forget' any files prior to modeling them and re-learn them afterwards.
	 */
	public static void selfTesting(boolean selfTesting) {
		ModelRunner.selfTesting = selfTesting;
	}
	
	/**
	 * Returns whether or not the model is set up to run self-testing (training on test-set)
	 */
	public static boolean isSelfTesting() {
		return ModelRunner.selfTesting;
	}
	
	/**
	 * Set the order for n-gram (and similar models). Default: 6.
	 * It is admittedly a bit inappropriate for this class to maintain such a specific
	 * parameter, but n-gram models are prevalent enough to endorse it in this one instance.
	 * 
	 * @param order The new order to use.
	 */
	public static void setNGramOrder(int order) {
		ngramOrder = order;
	}

	/**
	 * Return the n-gram modeling order used
	 */
	public static int getNGramOrder() {
		return ngramOrder;
	}
	
	/**
	 * Set the cut-off for the number of predictions to be returned by a model. Default: 10.
	 * 
	 * @param cutoff The new cut-off to be used.
	 */
	public static void setPredictionCutoff(int cutoff) {
		predictionCutoff = cutoff;
	}

	/**
	 * Return the maximum number of predictions a model should return.
	 */
	public static int getPredictionCutoff() {
		return predictionCutoff;
	}

	private static long[] learnCounts = new long[2];
	private static long[] learnTime = new long[1];

	public static void learn(Model model, File file) {
		learnCounts = new long[] { 0, 0 };
		learnTime = new long[] { -System.currentTimeMillis() };
		try {
			Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.peek(f -> {
					if (++learnCounts[0] % 1000 == 0) {
						System.out.println("Counting at file " + learnCounts[0] + ", tokens processed: " + learnCounts[1] + " in " + (learnTime[0] + System.currentTimeMillis())/1000 + "s");
					}
				})
				.forEach(f -> learnFile(model, f));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (learnCounts[0] > 1000 || learnCounts[1] > 1000000) {
			System.out.println("Counting complete, files: " + learnCounts[0] + ", tokens: " + learnCounts[1] + ", time: " + (learnTime[0] + System.currentTimeMillis())/1000 + "s");
		}
	}
	
	public static void learnFile(Model model, File f) {
		model.notify(f);
		learnTokens(model, LexerRunner.lex(f));
	}
	
	public static void learnLines(Model model, Stream<String> lines) {
		learnTokens(model, LexerRunner.lex(lines));
	}

	public static void learnTokens(Model model, Stream<Stream<String>> lexed) {
		if (perLine) {
			lexed.map(Vocabulary::toIndices)
				.map(l -> l.peek(l2 -> learnCounts[1]++))
				.map(l -> l.collect(Collectors.toList()))
				.forEach(model::learn);
		}
		else {
			model.learn(lexed.map(l -> l.peek(l2 -> learnCounts[1]++))
				.flatMap(Vocabulary::toIndices)
				.collect(Collectors.toList()));
		}
	}
	
	public static void forget(Model model, File file) {
		try {
			Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.forEach(f -> forgetFile(model, f));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void forgetLines(Model model, Stream<String> lines) {
		forgetTokens(model, LexerRunner.lex(lines));
	}
	
	public static void forgetFile(Model model, File f) {
		model.notify(f);
		forgetTokens(model, LexerRunner.lex(f));
	}

	public static void forgetTokens(Model model, Stream<Stream<String>> lexed) {
		if (perLine) {
			lexed.map(Vocabulary::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.forEach(model::forget);
		}
		else {
			model.forget(lexed.flatMap(Vocabulary::toIndices).collect(Collectors.toList()));
		}
	}

	private static long[] modelCounts = new long[2];
	private static long[] modelTime = new long[1];
	
	private static double[] ent = new double[1];
	private static double[] mrr = new double[1];
	
	public static Stream<Pair<File, List<List<Double>>>> model(Model model, File file) {
		modelCounts = new long[] { 0, 0 };
		modelTime = new long[] { -System.currentTimeMillis() };
		ent = new double[] { 0.0 };
		try {
			return Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.peek(f -> {
					if (++modelCounts[0] % 100 == 0) {
						System.out.printf("Modeling @ file %d (%d tokens, %ds), entropy: %.4f\n",
								modelCounts[0], modelCounts[1], (System.currentTimeMillis() + modelTime[0])/1000, ent[0]/modelCounts[1]);
					}
				})
				.map(f -> modelFile(model, f));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Pair<File, List<List<Double>>> modelFile(Model model, File f) {
		model.notify(f);
		List<List<Double>> lineProbs = modelTokens(model, LexerRunner.lex(f));
		return Pair.of(f, lineProbs);
	}

	public static List<List<Double>> modelLines(Model model, Stream<String> lines) {
		return modelTokens(model, LexerRunner.lex(lines));
	}

	public static List<List<Double>> modelTokens(Model model, Stream<Stream<String>> lexed) {
		Vocabulary.setCheckpoint();
		List<List<Double>> lineProbs;
		if (perLine) {
			lineProbs = lexed.map(Vocabulary::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.map(l -> modelSequence(model, l))
				.collect(Collectors.toList());
			DoubleSummaryStatistics stats = lineProbs.stream()
					.flatMap(l -> l.stream().skip(LexerRunner.addsSentenceMarkers() ? 1 : 0))
					.mapToDouble(l -> l).summaryStatistics();
			ent[0] += stats.getSum();
			modelCounts[1] += stats.getCount();
		} else {
			List<Integer> lineLengths = new ArrayList<>();
			List<Double> modeled = modelSequence(model, lexed
				.map(Vocabulary::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.peek(l -> lineLengths.add(l.size()))
				.flatMap(l -> l.stream()).collect(Collectors.toList()));
			DoubleSummaryStatistics stats = modeled.stream()
					.skip(LexerRunner.addsSentenceMarkers() ? 1 : 0)
					.mapToDouble(l -> l).summaryStatistics();
			ent[0] += stats.getSum();
			modelCounts[1] += stats.getCount();
			lineProbs = toLines(modeled, lineLengths);
		}
		Vocabulary.restoreCheckpoint();
		return lineProbs;
	}

	private static List<Double> modelSequence(Model model, List<Integer> tokens) {
		if (selfTesting) model.forget(tokens);
		List<Double> entropies = model.model(tokens).stream()
			.map(ModelRunner::toProb)
			.map(ModelRunner::toEntropy)
			.collect(Collectors.toList());
		if (selfTesting) model.learn(tokens);
		return entropies;
	}

	public static Stream<Pair<File, List<List<Double>>>> predict(Model model, File file) {
		modelCounts = new long[] { 0, 0 };
		modelTime = new long[] { -System.currentTimeMillis() };
		mrr = new double[]  { 0.0 };
		try {
			return Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.peek(f -> {
					if (++modelCounts[0] % 100 == 0) {
						System.out.printf("Predicting @ file %d (%d tokens, %ds), mrr: %.4f\n",
							modelCounts[0], modelCounts[1], (System.currentTimeMillis() + modelTime[0])/1000, mrr[0]/modelCounts[1]);
					}
				})
				.map(f -> predictFile(model, f));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Pair<File, List<List<Double>>> predictFile(Model model, File f) {
		model.notify(f);
		List<List<Double>> lineProbs = predictTokens(model, LexerRunner.lex(f));
		return Pair.of(f, lineProbs);
	}

	public static List<List<Double>> predictLines(Model model, Stream<String> lines) {
		return predictTokens(model, LexerRunner.lex(lines));
	}

	public static List<List<Double>> predictTokens(Model model, Stream<Stream<String>> lexed) {
		Vocabulary.setCheckpoint();
		List<List<Double>> lineProbs;
		if (perLine) {
			lineProbs = lexed
				.map(Vocabulary::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.map(l -> predictSequence(model, l))
				.collect(Collectors.toList());
			DoubleSummaryStatistics stats = lineProbs.stream()
					.flatMap(l -> l.stream().skip(LexerRunner.addsSentenceMarkers() ? 1 : 0))
					.mapToDouble(l -> l).summaryStatistics();
			mrr[0] += stats.getSum();
			modelCounts[0] += stats.getCount();
		} else {
			List<Integer> lineLengths = new ArrayList<>();
			List<Double> modeled = predictSequence(model, lexed
				.map(Vocabulary::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.peek(l -> lineLengths.add(l.size()))
				.flatMap(l -> l.stream()).collect(Collectors.toList()));
			lineProbs = toLines(modeled, lineLengths);
			DoubleSummaryStatistics stats = modeled.stream()
					.skip(LexerRunner.addsSentenceMarkers() ? 1 : 0)
					.mapToDouble(l -> l).summaryStatistics();
			mrr[0] += stats.getSum();
			modelCounts[0] += stats.getCount();
		}
		Vocabulary.restoreCheckpoint();
		return lineProbs;
	}
	
	private static List<Double> predictSequence(Model model, List<Integer> tokens) {
		if (selfTesting) model.forget(tokens);
		List<List<Integer>> preds = toPredictions(model.predict(tokens));
		List<Double> mrrs = IntStream.range(0, tokens.size())
				.mapToObj(i -> preds.get(i).indexOf(tokens.get(i)))
				.map(ix -> ix >= 0 ? 1.0 / (ix + 1) : 0.0)
				.collect(Collectors.toList());
		if (selfTesting) model.learn(tokens);
		return mrrs;
	}

	public static List<Double> toProb(List<Pair<Double, Double>> probConfs) {
		return probConfs.stream().map(ModelRunner::toProb).collect(Collectors.toList());
	}
	
	public static double toProb(Pair<Double, Double> probConf) {
		return probConf.left*probConf.right + (1 - probConf.right)/Vocabulary.size();
	}
	
	public static double toEntropy(double probability) {
		return Math.log(probability)/NEG_LOG_2;
	}
	
	public static List<List<Integer>> toPredictions(List<Map<Integer, Pair<Double, Double>>> probConfs) {
		return probConfs.stream().map(ModelRunner::toPredictions).collect(Collectors.toList());
	}
	
	public static List<Integer> toPredictions(Map<Integer, Pair<Double, Double>> probConf) {
		return probConf.entrySet().stream()
			.map(e -> Pair.of(e.getKey(), toProb(e.getValue())))
			.sorted((p1, p2) -> -Double.compare(p1.right, p2.right))
			.map(p -> p.left)
			.collect(Collectors.toList());
	}

	private static List<List<Double>> toLines(List<Double> modeled, List<Integer> lineLengths) {
		List<List<Double>> perLine = new ArrayList<>();
		int ix = 0;
		for (int i = 0; i < lineLengths.size(); i++) {
			List<Double> line = new ArrayList<>();
			for (int j = 0; j < lineLengths.get(i); j++) {
				line.add(modeled.get(ix++));
			}
			perLine.add(line);
		}
		return perLine;
	}

	// TODO, invoke next method if possible
	public static DoubleSummaryStatistics getStats(Stream<Pair<File, List<List<Double>>>> fileProbs) {
		boolean skip = LexerRunner.addsSentenceMarkers();
		if (LexerRunner.isPerLine()) {
			return fileProbs.flatMap(p -> p.right.stream())
					.flatMap(l -> l.stream().skip(skip ? 1 : 0))
					.mapToDouble(p -> p).summaryStatistics();
		}
		else {
			return fileProbs.flatMap(p -> p.right.stream()
					.flatMap(l -> l.stream()).skip(skip ? 1 : 0))
					.mapToDouble(p -> p).summaryStatistics();
		}
	}
	
	public static DoubleSummaryStatistics getStats(List<List<Double>> fileProbs) {
		boolean skip = LexerRunner.addsSentenceMarkers();
		if (LexerRunner.isPerLine()) {
			return fileProbs.stream()
					.flatMap(l -> l.stream().skip(skip ? 1 : 0))
					.mapToDouble(p -> p).summaryStatistics();
		}
		else {
			return fileProbs.stream()
					.flatMap(l -> l.stream()).skip(skip ? 1 : 0)
					.mapToDouble(p -> p).summaryStatistics();
		}
	}
		
}
