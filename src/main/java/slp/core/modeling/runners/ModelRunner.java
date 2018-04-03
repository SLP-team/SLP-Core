package slp.core.modeling.runners;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import slp.core.lexing.LexerRunner;
import slp.core.modeling.Model;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;

/**
 * The this class provides the third step in modeling (after lexing, translating).
 * It can be configured statically and exposes modeling methods for convenience.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class ModelRunner {
	
	protected final LexerRunner lexerRunner;
	
	private static final double INV_NEG_LOG_2 = -1.0/Math.log(2);
	public static final int DEFAULT_NGRAM_ORDER = 6;
	
	private boolean perLine = false;
	private boolean selfTesting = false;
	
	public static int GLOBAL_PREDICTION_CUTOFF = 10;

	public ModelRunner() {
		this(new LexerRunner());
	}
	
	public ModelRunner(LexerRunner lexerRunner) {
		this.lexerRunner = lexerRunner;
	}
	
	public LexerRunner getLexerRunner() {
		return this.lexerRunner;
	}
	
	public static int getPredictionCutoff() {
		return GLOBAL_PREDICTION_CUTOFF;
	}

	public static void setPredictionCutoff(int cutoff) {
		GLOBAL_PREDICTION_CUTOFF = cutoff;
	}

	public Vocabulary getVocabulary() {
		return this.lexerRunner.getVocabulary();
	}

	/**
	 * Treat each line separately (default: false).
	 * <br />
	 * <em>Note:</em> does not in any way interact with {@link LexerRunner#perLine()}!
	 * So if the lexer does not append line delimiters, this code won't do so either
	 * but will still run on each line separately.
	 * 
	 * @param perLine 
	 */
	public void perLine(boolean perLine) {
		this.perLine = perLine;
	}
	
	/**
	 * Returns whether or not this class will model each line in isolation.
	 */
	public boolean isPerLine() {
		return this.perLine;
	}
	
	/**
	 * Indicate that we are testing on the training set,
	 * which means we must 'forget' any files prior to modeling them and re-learn them afterwards.
	 */
	public void selfTesting(boolean selfTesting) {
		this.selfTesting = selfTesting;
	}
	
	/**
	 * Returns whether or not the model is set up to run self-testing (training on test-set)
	 */
	public boolean isSelfTesting() {
		return this.selfTesting;
	}

	protected void notify(Model model, File f) {
		model.notify(f);
	}

	private final long LEARN_PRINT_INTERVAL = 1000000;
	private long[] learnStats = new long[2];

	public void learn(Model model, File file) {
		this.learnStats = new long[] { 0, -System.currentTimeMillis() };
		try {
			Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.forEach(f -> learnFile(model, f));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (this.learnStats[0] > LEARN_PRINT_INTERVAL && this.learnStats[1] > 0) {
			System.out.printf("Counting complete: %d tokens processed in %ds\n",
					this.learnStats[0], (System.currentTimeMillis() + this.learnStats[1])/1000);
		}
	}
	
	public void learnFile(Model model, File f) {
		notify(model, f);
		learnTokens(model, this.lexerRunner.lex(f));
	}

	public void learnContent(Model model, String content) {
		learnTokens(model, this.lexerRunner.lex(content));
	}

	public void learnLines(Model model, String[] lines) {
		learnLines(model, Arrays.stream(lines));
	}
	public void learnLines(Model model, List<String> lines) {
		learnLines(model, lines.stream());
	}
	public void learnLines(Model model, Stream<String> lines) {
		learnTokens(model, this.lexerRunner.lex(lines));
	}

	public void learnTokens(Model model, Stream<Stream<String>> lexed) {
		if (this.perLine) {
			lexed.map(this.getVocabulary()::toIndices)
				.map(l -> l.peek(l2 -> {
					if (++this.learnStats[0] % LEARN_PRINT_INTERVAL == 0 && this.learnStats[1] > 0) {
						System.out.printf("Counting: %dM tokens processed in %ds\n",
								Math.round(this.learnStats[0]/1e6),
								(System.currentTimeMillis() + this.learnStats[1])/1000);
					}
				}))
				.map(l -> l.collect(Collectors.toList()))
				.forEach(model::learn);
		}
		else {
			model.learn(lexed.map(l -> l.peek(l2 -> {
					if (++this.learnStats[0] % LEARN_PRINT_INTERVAL == 0 && this.learnStats[1] > 0) {
						System.out.printf("Counting: %dM tokens processed in %ds\n",
								Math.round(this.learnStats[0]/1e6),
								(System.currentTimeMillis() + this.learnStats[1])/1000);
					}
				}))
				.flatMap(this.getVocabulary()::toIndices)
				.collect(Collectors.toList()));
		}
	}
	
	public void forget(Model model, File file) {
		try {
			Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.forEach(f -> forgetFile(model, f));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void forgetFile(Model model, File f) {
		notify(model, f);
		forgetTokens(model, this.lexerRunner.lex(f));
	}
	
	public void forgetContent(Model model, String content) {
		forgetTokens(model, this.lexerRunner.lex(content));
	}

	public void forgetLines(Model model, String[] lines) {
		forgetLines(model, Arrays.stream(lines));
	}
	public void forgetLines(Model model, List<String> lines) {
		forgetLines(model, lines.stream());
	}
	public void forgetLines(Model model, Stream<String> lines) {
		forgetTokens(model, this.lexerRunner.lex(lines));
	}
	
	public void forgetTokens(Model model, Stream<Stream<String>> lexed) {
		if (this.perLine) {
			lexed.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.forEach(model::forget);
		}
		else {
			model.forget(lexed.flatMap(this.getVocabulary()::toIndices).collect(Collectors.toList()));
		}
	}

	private final int MODEL_PRINT_INTERVAL = 100000;
	private long[] modelStats = new long[2];
	private double[] ent = new double[1];
	private double[] mrr = new double[1];
	
	public Stream<Pair<File, List<List<Double>>>> model(Model model, File file) {
		this.modelStats = new long[] { 0, -System.currentTimeMillis()  };
		this.ent = new double[] { 0.0 };
		try {
			return Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.map(f -> Pair.of(f, modelFile(model, f)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<List<Double>> modelFile(Model model, File f) {
		notify(model, f);
		List<List<Double>> lineProbs = modelTokens(model, this.lexerRunner.lex(f));
		return lineProbs;
	}

	public List<List<Double>> modelContent(Model model, String content) {
		return modelTokens(model, this.lexerRunner.lex(content));
	}

	public List<List<Double>> modelLines(Model model, String[] lines) {
		return modelLines(model, Arrays.stream(lines));
	}
	public List<List<Double>> modelLines(Model model, List<String> lines) {
		return modelLines(model, lines.stream());
	}
	public List<List<Double>> modelLines(Model model, Stream<String> lines) {
		return modelTokens(model, this.lexerRunner.lex(lines));
	}

	public List<List<Double>> modelTokens(Model model, Stream<Stream<String>> lexed) {
		this.getVocabulary().setCheckpoint();
		List<List<Double>> lineProbs;
		if (this.perLine) {
			lineProbs = lexed.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.map(l -> modelSequence(model, l))
				.peek(l -> {
					DoubleSummaryStatistics stats = l.stream()
							.skip(this.lexerRunner.hasSentenceMarkers() ? 1 : 0)
							.mapToDouble(Double::doubleValue).summaryStatistics();
					long prevCount = this.modelStats[0];
					this.modelStats[0] += stats.getCount();
					this.ent[0] += stats.getSum();
					if (this.modelStats[0] / this.MODEL_PRINT_INTERVAL > prevCount / this.MODEL_PRINT_INTERVAL
							&& this.modelStats[1] > 0) {
						System.out.printf("Modeling: %dK tokens processed in %ds, avg. entropy: %.4f\n",
								Math.round(this.modelStats[0]/1e3),
								(System.currentTimeMillis() + this.modelStats[1])/1000, this.ent[0]/this.modelStats[0]);
					}
				})
				.collect(Collectors.toList());
		} else {
			List<Integer> lineLengths = new ArrayList<>();
			List<Double> modeled = modelSequence(model, lexed
				.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.peek(l -> lineLengths.add(l.size()))
				.flatMap(l -> l.stream()).collect(Collectors.toList()));
			lineProbs = toLines(modeled, lineLengths);
			DoubleSummaryStatistics stats = modeled.stream()
					.skip(this.lexerRunner.hasSentenceMarkers() ? 1 : 0)
					.mapToDouble(l -> l).summaryStatistics();
			long prevCount = this.modelStats[0];
			this.modelStats[0] += stats.getCount();
			this.ent[0] += stats.getSum();
			if (this.modelStats[0] / this.MODEL_PRINT_INTERVAL > prevCount / this.MODEL_PRINT_INTERVAL
					&& this.modelStats[1] > 0) {
				System.out.printf("Modeling: %dK tokens processed in %ds, avg. entropy: %.4f\n",
						Math.round(this.modelStats[0]/1e3),
						(System.currentTimeMillis() + this.modelStats[1])/1000, this.ent[0]/this.modelStats[0]);
			}
		}
		this.getVocabulary().restoreCheckpoint();
		return lineProbs;
	}

	protected List<Double> modelSequence(Model model, List<Integer> tokens) {
		if (this.selfTesting) model.forget(tokens);
		List<Double> entropies = model.model(tokens).stream()
			.map(this::toProb)
			.map(ModelRunner::toEntropy)
			.collect(Collectors.toList());
		if (this.selfTesting) model.learn(tokens);
		return entropies;
	}

	public Stream<Pair<File, List<List<Double>>>> predict(Model model, File file) {
		this.modelStats = new long[] { 0, -System.currentTimeMillis()  };
		this.mrr = new double[]  { 0.0 };
		try {
			return Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.map(f -> Pair.of(f, predictFile(model, f)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<List<Double>> predictFile(Model model, File f) {
		notify(model, f);
		List<List<Double>> lineProbs = predictTokens(model, this.lexerRunner.lex(f));
		return lineProbs;
	}


	public List<List<Double>> predictContent(Model model, String content) {
		return predictTokens(model, this.lexerRunner.lex(content));
	}

	public List<List<Double>> predictLines(Model model, String[] lines) {
		return predictLines(model, Arrays.stream(lines));
	}
	public List<List<Double>> predictLines(Model model, List<String> lines) {
		return predictLines(model, lines.stream());
	}
	public List<List<Double>> predictLines(Model model, Stream<String> lines) {
		return predictTokens(model, this.lexerRunner.lex(lines));
	}

	public List<List<Double>> predictTokens(Model model, Stream<Stream<String>> lexed) {
		this.getVocabulary().setCheckpoint();
		List<List<Double>> lineProbs;
		if (this.perLine) {
			lineProbs = lexed
				.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.map(l -> predictSequence(model, l))
				.peek(l -> {
					DoubleSummaryStatistics stats = l.stream()
							.skip(this.lexerRunner.hasSentenceMarkers() ? 1 : 0)
							.mapToDouble(Double::doubleValue).summaryStatistics();
					long prevCount = this.modelStats[0];
					this.modelStats[0] += stats.getCount();
					this.mrr[0] += stats.getSum();
					if (this.modelStats[0] / this.MODEL_PRINT_INTERVAL > prevCount / this.MODEL_PRINT_INTERVAL
							&& this.modelStats[1] > 0) {
						System.out.printf("Predicting: %dK tokens processed in %ds, avg. MRR: %.4f\n",
								Math.round(this.modelStats[0]/1e3),
								(System.currentTimeMillis() + this.modelStats[1])/1000, this.mrr[0]/this.modelStats[0]);
					}
				})
				.collect(Collectors.toList());
		} else {
			List<Integer> lineLengths = new ArrayList<>();
			List<Double> modeled = predictSequence(model, lexed
				.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.peek(l -> lineLengths.add(l.size()))
				.flatMap(l -> l.stream()).collect(Collectors.toList()));
			lineProbs = toLines(modeled, lineLengths);
			DoubleSummaryStatistics stats = modeled.stream()
					.skip(this.lexerRunner.hasSentenceMarkers() ? 1 : 0)
					.mapToDouble(l -> l).summaryStatistics();
			long prevCount = this.modelStats[0];
			this.modelStats[0] += stats.getCount();
			this.mrr[0] += stats.getSum();
			if (this.modelStats[0] / this.MODEL_PRINT_INTERVAL > prevCount / this.MODEL_PRINT_INTERVAL
					&& this.modelStats[1] > 0) {
				System.out.printf("Predicting: %dK tokens processed in %ds, avg. MRR: %.4f\n",
						Math.round(this.modelStats[0]/1e3),
						(System.currentTimeMillis() + this.modelStats[1])/1000, this.mrr[0]/this.modelStats[0]);
			}
		}
		this.getVocabulary().restoreCheckpoint();
		return lineProbs;
	}
	
	protected List<Double> predictSequence(Model model, List<Integer> tokens) {
		if (this.selfTesting) model.forget(tokens);
		List<List<Integer>> preds = toPredictions(model.predict(tokens));
		List<Double> mrrs = IntStream.range(0, tokens.size())
				.mapToObj(i -> preds.get(i).indexOf(tokens.get(i)))
				.map(ModelRunner::toMRR)
				.collect(Collectors.toList());
		if (this.selfTesting) model.learn(tokens);
		return mrrs;
	}

	public List<Double> toProb(List<Pair<Double, Double>> probConfs) {
		return probConfs.stream().map(this::toProb).collect(Collectors.toList());
	}
	
	public double toProb(Pair<Double, Double> probConf) {
		return probConf.left*probConf.right + (1 - probConf.right)/this.getVocabulary().size();
	}
	
	public static double toEntropy(double probability) {
		return Math.log(probability) * INV_NEG_LOG_2;
	}
	
	public static double toMRR(Integer ix) {
		return ix >= 0 ? 1.0 / (ix + 1) : 0.0;
	}

	public List<List<Integer>> toPredictions(List<Map<Integer, Pair<Double, Double>>> probConfs) {
		return probConfs.stream().map(this::toPredictions).collect(Collectors.toList());
	}
	
	public List<Integer> toPredictions(Map<Integer, Pair<Double, Double>> probConf) {
		return probConf.entrySet().stream()
			.map(e -> Pair.of(e.getKey(), toProb(e.getValue())))
			.sorted((p1, p2) -> -Double.compare(p1.right, p2.right))
			.limit(GLOBAL_PREDICTION_CUTOFF)
			.map(p -> p.left)
			.collect(Collectors.toList());
	}

	private <K> List<List<K>> toLines(List<K> modeled, List<Integer> lineLengths) {
		List<List<K>> perLine = new ArrayList<>();
		int ix = 0;
		for (int i = 0; i < lineLengths.size(); i++) {
			List<K> line = new ArrayList<>();
			for (int j = 0; j < lineLengths.get(i); j++) {
				line.add(modeled.get(ix++));
			}
			perLine.add(line);
		}
		return perLine;
	}

	public DoubleSummaryStatistics getStats(Map<File, List<List<Double>>> fileProbs) {
		return getStats(fileProbs.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue())));
	}
	
	// TODO, invoke next method if possible
	public DoubleSummaryStatistics getStats(Stream<Pair<File, List<List<Double>>>> fileProbs) {
		boolean skip = this.lexerRunner.hasSentenceMarkers();
		if (this.lexerRunner.isPerLine()) {
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
	
	public DoubleSummaryStatistics getStats(List<List<Double>> fileProbs) {
		boolean skip = this.lexerRunner.hasSentenceMarkers();
		if (this.lexerRunner.isPerLine()) {
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
