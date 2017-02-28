package slp.core.modeling;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.CounterRunner;
import slp.core.counting.Vocabulary;
import slp.core.io.Reader;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Configuration;
import slp.core.util.Util;

public class ModelRunner {

	private final boolean dynamic;
	private final boolean self;
	private final boolean nested;
	private final int cacheSize;

	public ModelRunner() {
		this(CacheModel.DEFAULT_CAPACITY);
	}
	
	public ModelRunner(int cacheSize) {
		this(cacheSize, false);
	}
	
	public ModelRunner(int cacheSize, boolean self) {
		this(cacheSize, self, false);
	}
	
	public ModelRunner(int cacheSize, boolean self, boolean nested) {
		this(cacheSize, self, nested, false);
	}
	
	public ModelRunner(int cacheSize, boolean self, boolean nested, boolean dynamic) {
		this.cacheSize = cacheSize;
		this.self = self;
		this.nested = nested;
		this.dynamic = dynamic;
	}
	
	public Map<File, List<Double>> model(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter) {
		return model(testRoot, vocabulary, tokenizer, Sequencer.standard(), counter);
	}

	public Map<File, List<Double>> model(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter, Model model) {
		return model(testRoot, vocabulary, tokenizer, Sequencer.standard(), counter, model);
	}

	public Map<File, List<Double>> model(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Sequencer sequencer, Counter counter) {
		return model(testRoot, vocabulary, tokenizer, sequencer, counter, Model.standard(counter));
	}

	public Map<File, List<Double>> model(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Sequencer sequencer,
			Counter counter, Model model) {
		if (this.nested) {
			return modelNested(testRoot, vocabulary, tokenizer, counter, model);
		}
		else {
			return modelSimple(testRoot, vocabulary, tokenizer, counter, model);
		}
	}

	public Map<File, List<Double>> predict(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter) {
		return predict(testRoot, vocabulary, tokenizer, Sequencer.standard(), counter);
	}

	public Map<File, List<Double>> predict(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter, Model model) {
		return predict(testRoot, vocabulary, tokenizer, Sequencer.standard(), counter, model);
	}

	public Map<File, List<Double>> predict(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Sequencer sequencer, Counter counter) {
		return predict(testRoot, vocabulary, tokenizer, sequencer, counter, Model.standard(counter));
	}
	
	public Map<File, List<Double>> predict(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Sequencer sequencer,
			Counter counter, Model model) {
		if (this.nested) {
			return predictNested(testRoot, vocabulary, tokenizer, counter, model);
		}
		else {
			return predictSimple(testRoot, vocabulary, tokenizer, counter, model);
		}
	}
	
	private static final double log2 = Math.log(2);

	private Map<File, List<Double>> modelSimple(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter, Model model) {
		List<File> files = Util.getFiles(testRoot);
		double entropy = 0.0;
		int count = 0;
		if (Configuration.closed()) vocabulary.close();
		Map<File, List<Double>> results = new LinkedHashMap<>();
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			if (this.self) CounterRunner.count(file, counter, tokenizer, vocabulary, false);
			if (!Configuration.closed()) vocabulary.setCheckpoint();
			
			CacheModel cache = this.cacheSize > 0 ? new CacheModel(this.cacheSize) : new CacheModel(0);
			Model mix = new MixModel(model, cache);
			List<Double> stats = model(file, mix, tokenizer, vocabulary);
			results.put(file, stats);
			entropy += stats.stream().mapToDouble(x -> x).sum();
			count += stats.size();
			
			if (!Configuration.closed()) vocabulary.restoreCheckpoint();
			if (this.self || this.dynamic) CounterRunner.count(file, counter, tokenizer, vocabulary, true);
			// Intermediate logging
			if (files.size() < 100 || (i + 1) % 100 == 0) {
				System.out.println("At file " + (i + 1) + ":\t" + entropy / count + "\t" + count);
			}
		}
		System.out.println("Result: " + entropy / count + "\t" + count);
		return results;
	}

	private Map<File, List<Double>> modelNested(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter, Model model) {
		return modelNested(testRoot, vocabulary, tokenizer, counter, model, 0);
	}
	
	/**
	 * this.self-testing complicates this a little; basically when this.self-testing we don't do anything at the root.
	 * Instead, we push the global counter down to the lower level and then proceed as normal, with one caveat:
	 * - if there are single files right below the root AND we are this.self-counting,
	 * 	 we should unsee those events with the global counter instead of with the local one.
	 */
	private Map<File, List<Double>> modelNested(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter,
			Model global, int depth) {
		if (testRoot.list().length == 1) {
			File only = new File(testRoot, testRoot.list()[0]);
			if (!only.isFile()) {
				return modelNested(only, vocabulary, tokenizer, counter, global, depth);
			} else if (this.self && depth == 0) {
				System.out.println("Can't this.self-test on single file, exiting");
				return new LinkedHashMap<>();
			}
		}
		if (depth == 0 && Configuration.closed()) vocabulary.close();
		List<File> files = Util.getFiles(testRoot);
		Counter c = Counter.standard();
		Model local = Model.standard(c);
		Model mix = global;
		// Never un-count events at root (if this.self-testing, obvious, else counter hasn't seen these things)
		if (depth > 0) {
			if (!this.dynamic) files.stream().forEach(file -> CounterRunner.count(file, counter, tokenizer, vocabulary, false));
		}
		// Open the vocabulary for new event provided the original counter has un-seen all it needed to (in its own dialect)
		if (!Configuration.closed() && (!this.self || depth == 1)) vocabulary.setCheckpoint();
		// Populate the local counter for deeper packages as well as for the top one if not this.self-testing
		if (!this.self || depth > 0) {
			if (!this.dynamic) files.stream().forEach(file -> CounterRunner.count(file, c, tokenizer, vocabulary, true));
			mix = new MixModel(global, local);
		}
		Map<File, List<Double>> results = new LinkedHashMap<>();
		double entropy = 0;
		int count = 0;
		for (String child : testRoot.list()) {
			File file = new File(testRoot, child);
			if (file.isFile()) {
				CacheModel cache = this.cacheSize > 0 ? new CacheModel(this.cacheSize) : new CacheModel(0);
				Model mixed = new MixModel(mix, cache);
				// Unsee file with local counter unless we are both this.self-testing and at the root (local counter is empty)
				if (this.self && depth == 0) CounterRunner.count(file, counter, tokenizer, vocabulary, false);
				else if (!this.dynamic) CounterRunner.count(file, c, tokenizer, vocabulary, false);
				
				List<Double> stats = model(file, mixed, tokenizer, vocabulary);
				results.put(file, stats);
				entropy += stats.stream().mapToDouble(x -> x).sum();
				count += stats.size();
				
				// Same for re-seeing file, with added "dynamic" condition
				if (this.dynamic) CounterRunner.count(file, c, tokenizer, vocabulary, true);
				else if (this.self && depth == 0) CounterRunner.count(file, counter, tokenizer, vocabulary, true);
				else CounterRunner.count(file, c, tokenizer, vocabulary, true);
			}
			else {
				// Pass down local counter unless we are this.self-testing and at the root (local counter is empty)
				Counter toPassDown = this.self && depth == 0 ? counter : c;
				Map<File, List<Double>> lower = modelNested(file, vocabulary, tokenizer, toPassDown, mix, depth + 1);
				results.putAll(lower);
				entropy += lower.values().stream().flatMapToDouble(x -> x.stream().mapToDouble(y -> y)).sum();
				count += lower.values().stream().mapToInt(v -> v.size()).sum();
			}
		}
		if (!Configuration.closed() && (!this.self || depth == 1)) vocabulary.restoreCheckpoint();
		if (depth > 0) {
			files.stream().forEach(file -> CounterRunner.count(file, counter, tokenizer, vocabulary, true));
		}
		if (depth == 0) {
			System.out.println("Result: " + entropy / count + "\t" + count);
		}
		else if (depth == 1) {
			System.out.println("At file " + testRoot + ":\t" + entropy / count + "\t" + count);
		}
		return results;
	}
	
	private List<Double> model(File file, Model model, Tokenizer tokenizer, Vocabulary vocabulary) {
		return model(file, model, tokenizer, vocabulary, Sequencer.standard());
	}

	private List<Double> model(File file, Model model, Tokenizer tokenizer, Vocabulary vocabulary, Sequencer sequencer) {
		return Stream.of(Reader.readContent(file))
			.map(tokenizer::tokenize)
			.map(vocabulary::toIndices)
			.flatMap(sequencer::sequenceBackward)
			.map(model::model)
			.map(x -> -Math.log(x)/log2)
			.collect(Collectors.toList());
	}

	private Map<File, List<Double>> predictSimple(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter, Model model) {
		List<File> files = Util.getFiles(testRoot);
		int[] stats = new int[Configuration.predictionCutoff() + 1];
		if (Configuration.closed()) vocabulary.close();
		Map<File, List<Double>> results = new LinkedHashMap<>();
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			if (this.self) CounterRunner.count(file, counter, tokenizer, vocabulary, false);
			if (!Configuration.closed()) vocabulary.setCheckpoint();
			
			CacheModel cache = this.cacheSize > 0 ? new CacheModel(this.cacheSize) : new CacheModel(0);
			Model mix = new MixModel(model, cache);
			List<Double> sts = predict(file, Configuration.predictionCutoff(), mix, tokenizer, vocabulary);
			results.put(file, sts);
			for (double val : sts) {
				stats[val > 0 ? (int) Math.round(1.0/val) : 0]++;
			}
			
			if (!Configuration.closed()) vocabulary.restoreCheckpoint();
			if (this.self || this.dynamic) CounterRunner.count(file, counter, tokenizer, vocabulary, true);
			// Intermediate logging
			if (files.size() < 100 || (i + 1) % 100 == 0) {
				int sum = Arrays.stream(stats).sum();
				double partial = 0.0;
				System.out.print("At file " + (i + 1) + ":\t");
				for (int j = 1; j < stats.length; j++) {
					partial += stats[j];
					System.out.printf("%.4f\t", partial / sum);
				}
				DoubleSummaryStatistics mrr = getMRR(stats);
				System.out.printf("%.4f\n", mrr.getSum() / mrr.getCount());
			}
		}
		int sum = Arrays.stream(stats).sum();
		double partial = 0.0;
		System.out.print("Result:\t");
		for (int i = 1; i < stats.length; i++) {
			partial += stats[i];
			System.out.printf("%.4f\t", partial / sum);
		}
		DoubleSummaryStatistics mrr = getMRR(stats);
		System.out.printf("%.4f\n", mrr.getSum() / mrr.getCount());
		return results;
	}

	private Map<File, List<Double>> predictNested(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter, Model model) {
		return predictNested(testRoot, vocabulary, tokenizer, counter, model, 0);
	}

	private Map<File, List<Double>> predictNested(File testRoot, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter,
			Model global, int depth) {
		if (testRoot.list().length == 1) {
			File only = new File(testRoot, testRoot.list()[0]);
			if (!only.isFile()) {
				return predictNested(only, vocabulary, tokenizer, counter, global, depth);
			} else if (this.self && depth == 0) {
				System.out.println("Can't this.self-test on single file, exiting");
				return new LinkedHashMap<>();
			}
		}
		if (depth == 0 && Configuration.closed()) vocabulary.close();
		List<File> files = Util.getFiles(testRoot);
		Counter c = Counter.standard();
		Model local = Model.standard(c);
		Model mix = global;
		// Never un-count events at root (if this.self-testing, obvious, else counter hasn't seen these things)
		if (depth > 0) {
			if (!this.dynamic) files.stream().forEach(file -> CounterRunner.count(file, counter, tokenizer, vocabulary, false));
		}
		// Open the vocabulary for new event provided the original counter has un-seen all it needed to (in its own dialect)
		if (!Configuration.closed() && (!this.self || depth == 1)) vocabulary.setCheckpoint();
		// Populate the local counter for deeper packages as well as for the top one if not this.self-testing
		if (!this.self || depth > 0) {
			if (!this.dynamic) files.stream().forEach(file -> CounterRunner.count(file, c, tokenizer, vocabulary, true));
			mix = new MixModel(global, local);
		}
		Map<File, List<Double>> results = new LinkedHashMap<>();
		int[] stats = new int[Configuration.predictionCutoff() + 1];
		for (String child : testRoot.list()) {
			File file = new File(testRoot, child);
			if (file.isFile()) {
				CacheModel cache = this.cacheSize > 0 ? new CacheModel(this.cacheSize) : new CacheModel(0);
				Model mixed = new MixModel(mix, cache);
				// Unsee file with local counter unless we are both this.self-testing and at the root (local counter is empty)
				if (this.self && depth == 0) CounterRunner.count(file, counter, tokenizer, vocabulary, false);
				else if (!this.dynamic) CounterRunner.count(file, c, tokenizer, vocabulary, false);
				List<Double> sts = predict(file, Configuration.predictionCutoff(), mixed, tokenizer, vocabulary);
				results.put(file, sts);
				for (double val : sts) {
					stats[val > 0 ? (int) Math.round(1.0/val) : 0]++;
				}
				// Same for re-seeing file
				if (this.dynamic) CounterRunner.count(file, c, tokenizer, vocabulary, true);
				else if (this.self && depth == 0) CounterRunner.count(file, counter, tokenizer, vocabulary, true);
				else CounterRunner.count(file, c, tokenizer, vocabulary, true);
			}
			else {
				// Pass down local counter unless we are this.self-testing and at the root (local counter is empty)
				Counter toPassDown = this.self && depth == 0 ? counter : c;
				Map<File, List<Double>> lower = predictNested(file, vocabulary, tokenizer, toPassDown, mix, depth + 1);
				results.putAll(lower);
				for (List<Double> l : lower.values()) {
					for (double val : l) {
						stats[val > 0 ? (int) Math.round(1.0/val) : 0]++;
					}
				}
			}
		}
		if (!Configuration.closed() && (!this.self || depth == 1)) vocabulary.restoreCheckpoint();
		if (depth > 0) {
			files.stream().forEach(file -> CounterRunner.count(file, counter, tokenizer, vocabulary, true));
		}
		if (depth <= 1) {
			int sum = Arrays.stream(stats).sum();
			double partial = 0.0;
			if (depth == 1) System.out.print("At file " + testRoot + ":\t");
			else System.out.print("Result:\t");
			for (int i = 1; i < stats.length; i++) {
				partial += stats[i];
				System.out.printf("%.4f\t", partial / sum);
			}
			DoubleSummaryStatistics mrr = getMRR(stats);
			System.out.printf("%.4f\n", mrr.getSum() / mrr.getCount());
		}
		return results;
	}

	private List<Double> predict(File file, int limit, Model model, Tokenizer tokenizer, Vocabulary vocabulary) {
		return predict(file, limit, model, tokenizer, vocabulary, Sequencer.standard());
	}

	private List<Double> predict(File file, int limit, Model model, Tokenizer tokenizer, Vocabulary vocabulary, Sequencer sequencer) {
		return Stream.of(Reader.readContent(file))
			.map(tokenizer::tokenize)
			.map(vocabulary::toIndices)
			.flatMap(sequencer::sequenceBackward)
			.mapToInt(l -> getRank(model, l, limit))
			.mapToObj(i -> i > 0 ? 1.0 / i : 0.0)
			.collect(Collectors.toList());
	}

	private int getRank(Model model, List<Integer> in, int limit) {
		if (in.size() <= 1) return 0;
		List<Integer> ranked = model.predict(new ArrayList<>(in.subList(0, in.size() - 1)), limit);
		int rank = ranked.indexOf(in.get(in.size() - 1)) + 1;
		model.model(in);
		return rank;
	}

	public static DoubleSummaryStatistics getEntropy(List<Double> entropies) {
		return entropies.stream().mapToDouble(x -> x).summaryStatistics();
	}

	public static DoubleSummaryStatistics getMRR(int[] rankCounts) {
		DoubleSummaryStatistics stats = IntStream.range(0, rankCounts.length)
			.mapToObj(i -> IntStream.range(0, rankCounts[i]).mapToDouble(j -> i > 0 ? 1.0 / i : 0.0))
			.flatMapToDouble(x -> x)
			.summaryStatistics();
		return stats;
	}
}
