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

import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.ngram.NGramModel;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;

/**
 * The this class provides the third step in modeling (after lexing, translating).
 * It requires a {@linkplain LexerRunner} and {@linkplain Model} and exposes modeling methods for convenience.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class ModelRunner {
	
	private static final double INV_NEG_LOG_2 = -1.0/Math.log(2);
	public static final int DEFAULT_NGRAM_ORDER = 6;
	
	public static int GLOBAL_PREDICTION_CUTOFF = 10;
	
	protected final LexerRunner lexerRunner;
	protected final Model model;
	
	private boolean selfTesting = false;
	private Nester nester;
	
	public ModelRunner(LexerRunner lexerRunner, Model model) {
		this.lexerRunner = lexerRunner;
		this.model = model;
	}
	
	public LexerRunner getLexerRunner() {
		return this.lexerRunner;
	}
	
	public Model getModel() {
		return this.model;
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
	 * Indicate that we are testing on the training set,
	 * which means we must 'forget' any files prior to modeling them and re-learn them afterwards.
	 * <br/>
	 * If nesting is enabled, this may reconfigure the nesting setup to match.
	 * For better performance, set self-testing before enabling nesting.
	 */
	public void setSelfTesting(boolean selfTesting) {
		if (selfTesting != this.selfTesting) {
			this.selfTesting = selfTesting;
			// If self-testing has changed and this model is already nested,
			// we must re-calibrate the nester as well
			if (this.isNested()) this.setNested(this.nester.getTestRoot());
		}
	}
	
	/**
	 * Returns whether or not the model is set up to run self-testing (training on test-set)
	 */
	public boolean isSelfTesting() {
		return this.selfTesting;
	}
	
	/**
	 * <b>Important:</b> if self-testing is the goal, please enable it before enabling nested mode!
	 * Otherwise, self-testing will learn a model over all test data now.
	 * <br/>
	 * Sets the nature of this ModelRunner to automatically nest a model hierarchy around any file to be modeled.
	 * This hierarchy will be "rooted" in the highest test directory; hence the need to pass this as a parameter.
	 * 
	 * @param testRoot The directory within which all test files will reside.
	 * @param testBaseModel The model at testRoot.
	 */
	public void setNested(File testRoot) {
		if (this.selfTesting) {
			this.nester = new Nester(NGramModel.standard(), this.lexerRunner, testRoot, this.model);
		}
		else {
			Model testBaseModel = NGramModel.standard();
			new ModelRunner(this.lexerRunner, testBaseModel).learn(testRoot);
			this.nester = new Nester(this.model, this.lexerRunner, testRoot, testBaseModel);
		}
	}
	
	/**
	 * Returns whether or not the model is set up to run self-testing (training on test-set)
	 */
	public boolean isNested() {
		return this.nester != null;
	}
	
	public void notify(File f) {
		if (this.isNested()) this.nester.updateNesting(f);
		else this.model.notify(f);
	}

	private final long LEARN_PRINT_INTERVAL = 1000000;
	private long[] learnStats = new long[2];

	public void learn(File file) {
		this.learnStats = new long[] { 0, -System.currentTimeMillis() };
		try {
			Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.forEach(f -> learnFile(f));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (this.learnStats[0] > LEARN_PRINT_INTERVAL && this.learnStats[1] != 0) {
			System.out.printf("Counting complete: %d tokens processed in %ds\n",
					this.learnStats[0], (System.currentTimeMillis() + this.learnStats[1])/1000);
		}
	}
	
	public void learnFile(File f) {
		if (!f.getName().matches(this.lexerRunner.getRegex())) return;
		notify(f);
		learnTokens(this.lexerRunner.lex(f));
	}

	public void learnContent(String content) {
		learnTokens(this.lexerRunner.lex(content));
	}

	public void learnLines(String[] lines) {
		learnLines(Arrays.stream(lines));
	}
	public void learnLines(List<String> lines) {
		learnLines(lines.stream());
	}
	public void learnLines(Stream<String> lines) {
		learnTokens(this.lexerRunner.lex(lines));
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
	
	public void forget(File file) {
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
		if (!f.getName().matches(this.lexerRunner.getRegex())) return;
		notify(f);
		forgetTokens(this.lexerRunner.lex(f));
	}
	
	public void forgetContent(String content) {
		forgetTokens(this.lexerRunner.lex(content));
	}

	public void forgetLines(String[] lines) {
		forgetLines(Arrays.stream(lines));
	}
	public void forgetLines(List<String> lines) {
		forgetLines(lines.stream());
	}
	public void forgetLines(Stream<String> lines) {
		forgetTokens(this.lexerRunner.lex(lines));
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
	
	public Stream<Pair<File, List<List<Double>>>> model(File file) {
		this.modelStats = new long[] { 0, -System.currentTimeMillis()  };
		this.ent = 0.0;
		try {
			return Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.map(f -> Pair.of(f, modelFile(f)))
				.filter(p -> p != null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<List<Double>> modelFile(File f) {
		if (!f.getName().matches(this.lexerRunner.getRegex())) return null;
		notify(f);
		List<List<Double>> lineProbs = modelTokens(this.lexerRunner.lex(f));
		return lineProbs;
	}

	public List<List<Double>> modelContent(String content) {
		return modelTokens(this.lexerRunner.lex(content));
	}

	public List<List<Double>> modelLines(String[] lines) {
		return modelLines(Arrays.stream(lines));
	}
	public List<List<Double>> modelLines(List<String> lines) {
		return modelLines(lines.stream());
	}
	public List<List<Double>> modelLines(Stream<String> lines) {
		return modelTokens(this.lexerRunner.lex(lines));
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
		if (this.isNested()) {
			return this.nester.getMix().model(tokens).stream()
					.map(this::toProb)
					.map(ModelRunner::toEntropy)
					.collect(Collectors.toList());
		}
		else {
			if (this.selfTesting) this.model.forget(tokens);
			List<Double> entropies = this.model.model(tokens).stream()
				.map(this::toProb)
				.map(ModelRunner::toEntropy)
				.collect(Collectors.toList());
			if (this.selfTesting) this.model.learn(tokens);
			return entropies;
		}
	}

	private void logModelingProgress(List<Double> modeled) {
		DoubleSummaryStatistics stats = modeled.stream()
				.skip(this.lexerRunner.hasSentenceMarkers() ? 1 : 0)
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
		try {
			return Files.walk(file.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.map(f -> Pair.of(f, predictFile(f)))
				.filter(p -> p != null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<List<Double>> predictFile(File f) {
		if (!f.getName().matches(this.lexerRunner.getRegex())) return null;
		notify(f);
		List<List<Double>> lineProbs = predictTokens(this.lexerRunner.lex(f));
		return lineProbs;
	}


	public List<List<Double>> predictContent(String content) {
		return predictTokens(this.lexerRunner.lex(content));
	}

	public List<List<Double>> predictLines(String[] lines) {
		return predictLines(Arrays.stream(lines));
	}
	public List<List<Double>> predictLines(List<String> lines) {
		return predictLines(lines.stream());
	}
	public List<List<Double>> predictLines(Stream<String> lines) {
		return predictTokens(this.lexerRunner.lex(lines));
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
		if (this.isNested()) {
			List<List<Integer>> preds = toPredictions(this.nester.getMix().predict(tokens));
			return IntStream.range(0, tokens.size())
					.mapToObj(i -> preds.get(i).indexOf(tokens.get(i)))
					.map(ModelRunner::toMRR)
					.collect(Collectors.toList());
		}
		else {
			if (this.selfTesting) this.model.forget(tokens);
			List<List<Integer>> preds = toPredictions(this.model.predict(tokens));
			List<Double> mrrs = IntStream.range(0, tokens.size())
					.mapToObj(i -> preds.get(i).indexOf(tokens.get(i)))
					.map(ModelRunner::toMRR)
					.collect(Collectors.toList());
			if (this.selfTesting) this.model.learn(tokens);
			return mrrs;
		}
	}

	private void logPredictionProgress(List<Double> modeled) {
		DoubleSummaryStatistics stats = modeled.stream()
				.skip(this.lexerRunner.hasSentenceMarkers() ? 1 : 0)
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
		boolean skip = this.lexerRunner.hasSentenceMarkers();
		if (this.lexerRunner.isPerLine()) {
			return fileProbs.flatMap(List::stream)
					.flatMap(l -> l.stream().skip(skip ? 1 : 0))
					.mapToDouble(p -> p).summaryStatistics();
		}
		else {
			return fileProbs.flatMap(f -> f.stream()
						.flatMap(l -> l.stream())
						.skip(skip ? 1 : 0))
					.mapToDouble(p -> p).summaryStatistics();
		}
	}
}
