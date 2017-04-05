package core.modeling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import core.lexing.LexerRunner;
import core.translating.Vocabulary;
import core.util.Pair;

/**
 * The ModelRunner class provides the third step in modeling (after lexing, translating).
 * It can be configured statically and exposes static modeling methods for convenience.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class ModelRunner {
	
	private static final double NEG_LOG_2 = -Math.log(2);
	private static boolean useLines = false;
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
		ModelRunner.useLines = perLine;
	}
	
	/**
	 * Indicate that we are testing on the training set, which means we must un-count any files prior to modeling them.
	 */
	public static void selfTesting(boolean selfTesting) {
		ModelRunner.selfTesting = selfTesting;
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
	
	public static void learn(Model model, File file) {
		int[] count = { 0, 0 };
		try {
			Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.forEach(f -> {
					if (++count[0] % 1000 == 0) {
						System.out.println("Counting at file " + count[0] + ", tokens processed: " + count[1]);
					}
					if (useLines) {
						LexerRunner.lex(f).stream()
							.peek(l -> count[1] += l.size())
							.map(List::stream)
							.map(Vocabulary::toIndices)
							.map(l -> l.collect(Collectors.toList()))
							.forEach(model::learn);
					}
					else {
						model.learn(LexerRunner.lex(f).stream()
							.peek(l -> count[1] += l.size())
							.map(List::stream)
							.flatMap(Vocabulary::toIndices)
							.collect(Collectors.toList()));
					}
				});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void forget(Model model, File file) {
		try {
			Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.forEach(f -> {
					if (useLines) {
						LexerRunner.lex(f)
							.stream().map(List::stream)
							.map(Vocabulary::toIndices)
							.map(l -> l.collect(Collectors.toList()))
							.forEach(model::forget);
					}
					else {
						model.forget(LexerRunner.lex(f)
							.stream().map(List::stream)
							.flatMap(Vocabulary::toIndices)
							.collect(Collectors.toList()));
					}
				});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Stream<Pair<File, List<Double>>> model(Model model, File file) {
		int[] count = { 0, 0 };
		double[] ent = { 0.0 };
		try {
			return Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.map(f -> {
					if (++count[0] % 100 == 0) {
						System.out.printf("Modeling @ file %d (%d tokens), entropy:\t%.4f\n", count[0], count[1], ent[0]/count[1]);
					}
					model.notify(f);
					Vocabulary.setCheckpoint();
					Stream<Stream<Integer>> lines = LexerRunner.lex(f).stream()
						.peek(l -> count[1] += l.size())
						.map(List::stream)
						.map(Vocabulary::toIndices);
					List<Double> modeled = !useLines ? modelSequence(model, lines.flatMap(l -> l).collect(Collectors.toList()))
													 : lines.map(l -> l.collect(Collectors.toList()))
															.flatMap(l -> modelSequence(model, l).stream())
															.collect(Collectors.toList());
					ent[0] += modeled.stream().mapToDouble(m -> m).sum();
					Vocabulary.restoreCheckpoint();
					return Pair.of(f, modeled);
				});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static List<Double> modelSequence(Model model, List<Integer> tokens) {
		if (selfTesting) model.forget(tokens);
		List<Double> probabilities = model.model(tokens).stream()
			.map(ModelRunner::toProb)
			.map(ModelRunner::toEntropy)
			.collect(Collectors.toList());
		if (selfTesting) model.learn(tokens);
		return probabilities;
	}

	public static Stream<Pair<File, List<Double>>> predict(Model model, File file) {
		int[] count = { 0, 0 };
		double[] mrr = { 0.0 };
		try {
			return Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.map(f -> {
					if (++count[0] % 100 == 0) {
						System.out.printf("Predicting @ file %d (%d tokens), mrr:\t%.4f\n", count[0], count[1], mrr[0]/count[1]);
					}
					model.notify(f);
					Vocabulary.setCheckpoint();
					Stream<Stream<Integer>> lines = LexerRunner.lex(f).stream()
						.peek(l -> count[1] += l.size())
						.map(List::stream)
						.map(Vocabulary::toIndices);
					List<Double> modeled = !useLines ? predictSequence(model, lines.flatMap(l -> l).collect(Collectors.toList()))
													 : lines.map(l -> l.collect(Collectors.toList()))
															.flatMap(l -> predictSequence(model, l).stream())
															.collect(Collectors.toList());
					mrr[0] += modeled.stream().mapToDouble(m -> m).sum();
					Vocabulary.restoreCheckpoint();
					return Pair.of(f, modeled);
				});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
}
