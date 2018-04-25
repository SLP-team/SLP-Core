package slp.core.modeling.runners;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.ngram.NGramModel;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;

/**
 * This class can be used to run {@link Model}-related functions over bodies of code.
 * It provides the lexing and translation steps necessary to allow immediate learning and modeling from directories or files.
 * As such, it wraps the pipeline stages {@link Reader} --> {@link Lexer} --> Translate ({@link Vocabulary}) --> {@link Model}.
 * <br />
 * This class uses a {@link LexerRunner}, which differentiates between file and line data and provides a some additional utilities.
 * It also provides easier access to self-testing (in which each line is forgotten before modeling it and re-learned after),
 * which is helpful for count-based models such as {@link NGramModel}s.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class ModelRunner {
	
	private static final double INV_NEG_LOG_2 = -1.0/Math.log(2);
	public static final int DEFAULT_NGRAM_ORDER = 6;
	
	public static int GLOBAL_PREDICTION_CUTOFF = 10;
	
	protected final LexerRunner lexerRunner;
	protected final Vocabulary vocabulary;
	protected final Model model;

	private boolean selfTesting = false;
	
	public ModelRunner(Model model, LexerRunner lexer, Vocabulary vocabulary) {
		this.lexerRunner = lexer;
		this.vocabulary = vocabulary;
		this.model = model;
	}
	
	/**
	 * Convenience function that creates a new {@link ModelRunner} instance for the provided {@link Model}
	 * that is backed by the same {@link LexerRunner} and {@link Vocabulary}.
	 * 
	 * @param model The model to provide a {@link ModelRunner} for.
	 * @return A new {@link ModelRunner} for this {@link Model},
	 * 		   with the current {@link ModelRunner}'s {@link LexerRunner} and {@link Vocabulary}
	 */
	public ModelRunner copyForModel(Model model) {
		return new ModelRunner(model, this.lexerRunner, this.vocabulary);
	}
	
	public LexerRunner getLexerRunner() {
		return this.lexerRunner;
	}

	public Model getModel() {
		return this.model;
	}

	public Vocabulary getVocabulary() {
		return this.vocabulary;
	}

	/**
	 * Enables self testing: if we are testing on data that we also trained on, and our models are able to forget events,
	 * we can simulated training on all-but one sequence (the one we are modeling) by temporarily forgetting
	 * an event, modeling it and re-learning it afterwards. This maximizes use of context information and can be used
	 * to simulate full cross-validation.
	 * 
	 * @param selfTesting If true, will temporarily "forget" every sequence before modeling it and "re-learn" it afterwards
	 */
	public void setSelfTesting(boolean selfTesting) {
		this.selfTesting = selfTesting;
	}
	
	public static int getPredictionCutoff() {
		return GLOBAL_PREDICTION_CUTOFF;
	}

	public static void setPredictionCutoff(int cutoff) {
		GLOBAL_PREDICTION_CUTOFF = cutoff;
	}

	private final long LEARN_PRINT_INTERVAL = 1000000;
	private long[] learnStats = new long[2];

	public void learnDirectory(File file) {
		this.learnStats = new long[] { 0, -System.currentTimeMillis() };
		this.lexerRunner.lexDirectory(file)
			.forEach(p -> {
				this.model.notify(p.left);
				this.learnTokens(p.right);
			});
		if (this.learnStats[0] > LEARN_PRINT_INTERVAL && this.learnStats[1] != 0) {
			System.out.printf("Counting complete: %d tokens processed in %ds\n",
					this.learnStats[0], (System.currentTimeMillis() + this.learnStats[1])/1000);
		}
	}
	
	public void learnFile(File f) {
		if (!this.lexerRunner.willLexFile(f)) return;
		this.model.notify(f);
		learnTokens(this.lexerRunner.lexFile(f));
	}

	public void learnContent(String content) {
		learnTokens(this.lexerRunner.lexText(content));
	}

	public void learnTokens(Stream<Stream<String>> lexed) {
		if (this.lexerRunner.isPerLine()) {
			lexed.map(this.getVocabulary()::toIndices)
				.map(l -> l.peek(l2 -> logLearningProgress()))
				.map(l -> l.collect(Collectors.toList()))
				.forEach(this.model::learn);
		}
		else {
			this.model.learn(lexed.map(l -> l.peek(l2 -> logLearningProgress()))
				.flatMap(this.getVocabulary()::toIndices)
				.collect(Collectors.toList()));
		}
	}

	private void logLearningProgress() {
		if (++this.learnStats[0] % this.LEARN_PRINT_INTERVAL == 0 && this.learnStats[1] != 0) {
			System.out.printf("Counting: %dM tokens processed in %ds\n",
					Math.round(this.learnStats[0]/1e6),
					(System.currentTimeMillis() + this.learnStats[1])/1000);
		}
	}
	
	public void forgetDirectory(File file) {
		try {
			Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.forEach(f -> forgetFile(f));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void forgetFile(File f) {
		if (!this.lexerRunner.willLexFile(f)) return;
		this.model.notify(f);
		forgetTokens(this.lexerRunner.lexFile(f));
	}
	
	public void forgetContent(String content) {
		forgetTokens(this.lexerRunner.lexText(content));
	}
	
	public void forgetTokens(Stream<Stream<String>> lexed) {
		if (this.lexerRunner.isPerLine()) {
			lexed.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.forEach(this.model::forget);
		}
		else {
			this.model.forget(lexed.flatMap(this.getVocabulary()::toIndices).collect(Collectors.toList()));
		}
	}

	private final int MODEL_PRINT_INTERVAL = 100000;
	private long[] modelStats = new long[2];
	private double ent = 0.0;
	private double mrr = 0.0;
	
	public Stream<Pair<File, List<List<Double>>>> modelDirectory(File file) {
		this.modelStats = new long[] { 0, -System.currentTimeMillis()  };
		this.ent = 0.0;
		return this.lexerRunner.lexDirectory(file)
			.map(p -> {
				this.model.notify(p.left);
				return Pair.of(p.left, this.modelTokens(p.right));
			});
	}

	public List<List<Double>> modelFile(File f) {
		if (!this.lexerRunner.willLexFile(f)) return null;
		this.model.notify(f);
		return modelTokens(this.lexerRunner.lexFile(f));
	}

	public List<List<Double>> modelContent(String content) {
		return modelTokens(this.lexerRunner.lexText(content));
	}

	public List<List<Double>> modelTokens(Stream<Stream<String>> lexed) {
		this.getVocabulary().setCheckpoint();
		List<List<Double>> lineProbs;
		if (this.lexerRunner.isPerLine()) {
			lineProbs = lexed.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.map(l -> modelSequence(l))
				.peek(this::logModelingProgress)
				.collect(Collectors.toList());
		} else {
			List<Integer> lineLengths = new ArrayList<>();
			List<Double> modeled = modelSequence(lexed
				.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.peek(l -> lineLengths.add(l.size()))
				.flatMap(l -> l.stream()).collect(Collectors.toList()));
			lineProbs = toLines(modeled, lineLengths);
			logModelingProgress(modeled);
		}
		this.getVocabulary().restoreCheckpoint();
		return lineProbs;
	}

	protected List<Double> modelSequence(List<Integer> tokens) {
		if (this.selfTesting) this.model.forget(tokens);
		List<Double> entropies = this.model.model(tokens).stream()
			.map(this::toProb)
			.map(ModelRunner::toEntropy)
			.collect(Collectors.toList());
		if (this.selfTesting) this.model.learn(tokens);
		return entropies;
	}

	private void logModelingProgress(List<Double> modeled) {
		DoubleSummaryStatistics stats = modeled.stream().skip(1)
				.mapToDouble(Double::doubleValue).summaryStatistics();
		long prevCount = this.modelStats[0];
		this.modelStats[0] += stats.getCount();
		this.ent += stats.getSum();
		if (this.modelStats[0] / this.MODEL_PRINT_INTERVAL > prevCount / this.MODEL_PRINT_INTERVAL
				&& this.modelStats[1] != 0) {
			System.out.printf("Modeling: %dK tokens processed in %ds, avg. entropy: %.4f\n",
					Math.round(this.modelStats[0]/1e3),
					(System.currentTimeMillis() + this.modelStats[1])/1000, this.ent/this.modelStats[0]);
		}
	}

	public Stream<Pair<File, List<List<Double>>>> predict(File file) {
		this.modelStats = new long[] { 0, -System.currentTimeMillis()  };
		this.mrr = 0.0;
		return this.lexerRunner.lexDirectory(file)
			.map(p -> {
				this.model.notify(p.left);
				return Pair.of(p.left, this.predictTokens(p.right));
			});
	}

	public List<List<Double>> predictFile(File f) {
		if (!this.lexerRunner.willLexFile(f)) return null;
		this.model.notify(f);
		return predictTokens(this.lexerRunner.lexFile(f));
	}

	public List<List<Double>> predictContent(String content) {
		return predictTokens(this.lexerRunner.lexText(content));
	}

	public List<List<Double>> predictTokens(Stream<Stream<String>> lexed) {
		this.getVocabulary().setCheckpoint();
		List<List<Double>> lineProbs;
		if (this.lexerRunner.isPerLine()) {
			lineProbs = lexed
				.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.map(l -> predictSequence(l))
				.peek(this::logPredictionProgress)
				.collect(Collectors.toList());
		} else {
			List<Integer> lineLengths = new ArrayList<>();
			List<Double> modeled = predictSequence(lexed
				.map(this.getVocabulary()::toIndices)
				.map(l -> l.collect(Collectors.toList()))
				.peek(l -> lineLengths.add(l.size()))
				.flatMap(l -> l.stream()).collect(Collectors.toList()));
			lineProbs = toLines(modeled, lineLengths);
			logPredictionProgress(modeled);
		}
		this.getVocabulary().restoreCheckpoint();
		return lineProbs;
	}

	protected List<Double> predictSequence(List<Integer> tokens) {
		if (this.selfTesting) this.model.forget(tokens);
		List<List<Integer>> preds = toPredictions(this.model.predict(tokens));
		List<Double> mrrs = IntStream.range(0, tokens.size())
				.mapToObj(i -> preds.get(i).indexOf(tokens.get(i)))
				.map(ModelRunner::toMRR)
				.collect(Collectors.toList());
		if (this.selfTesting) this.model.learn(tokens);
		return mrrs;
	}

	private void logPredictionProgress(List<Double> modeled) {
		DoubleSummaryStatistics stats = modeled.stream().skip(1)
				.mapToDouble(Double::doubleValue).summaryStatistics();
		long prevCount = this.modelStats[0];
		this.modelStats[0] += stats.getCount();
		this.mrr += stats.getSum();
		if (this.modelStats[0] / this.MODEL_PRINT_INTERVAL > prevCount / this.MODEL_PRINT_INTERVAL
				&& this.modelStats[1] != 0) {
			System.out.printf("Predicting: %dK tokens processed in %ds, avg. MRR: %.4f\n",
					Math.round(this.modelStats[0]/1e3),
					(System.currentTimeMillis() + this.modelStats[1])/1000, this.mrr/this.modelStats[0]);
		}
	}

	public List<Double> toProb(List<Pair<Double, Double>> probConfs) {
		return probConfs.stream().map(this::toProb).collect(Collectors.toList());
	}
	
	public double toProb(Pair<Double, Double> probConf) {
		double prob = probConf.left;
		double conf = probConf.right;
		return prob*conf + (1 - conf)/this.getVocabulary().size();
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
	
	public DoubleSummaryStatistics getStats(Stream<Pair<File, List<List<Double>>>> fileProbs) {
		return getFileStats(fileProbs.map(p -> p.right));
	}
	
	public DoubleSummaryStatistics getStats(List<List<Double>> fileProbs) {
		return getFileStats(Stream.of(fileProbs));
	}
	
	private DoubleSummaryStatistics getFileStats(Stream<List<List<Double>>> fileProbs) {
		if (this.lexerRunner.isPerLine()) {
			return fileProbs.flatMap(List::stream)
					.flatMap(l -> l.stream().skip(1))
					.mapToDouble(p -> p).summaryStatistics();
		}
		else {
			return fileProbs.flatMap(f -> f.stream()
						.flatMap(l -> l.stream())
						.skip(1))
					.mapToDouble(p -> p).summaryStatistics();
		}
	}
}
