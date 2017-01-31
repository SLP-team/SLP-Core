package slp.core;

import java.io.File;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import slp.core.counting.Counter;
import slp.core.counting.CounterRunner;
import slp.core.counting.Vocabulary;
import slp.core.modeling.CacheModel;
import slp.core.modeling.MixModel;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Pair;
import slp.core.util.Util;

public class Testing {

	private static final int PREDICTION_CUTOFF = 10;

	static void test(String mode, File testDir, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter, Model model,
			int cacheSize, boolean self, boolean deep) {
		if (deep) {
			if (mode.equals("model")) testDeep(testDir, vocabulary, tokenizer, counter, model, cacheSize, self, 0);
			else predictDeep(testDir, vocabulary, tokenizer, counter, model, cacheSize, self, 0);
		}
		else {
			if (mode.equals("model")) testSimple(testDir, vocabulary, tokenizer, counter, model, cacheSize, self);
			else predictSimple(testDir, vocabulary, tokenizer, counter, model, cacheSize, self);
		}
	}
	
	/*
	 * Self-testing complicates this a little; basically when self-testing we don't do anything at the root.
	 * Instead, we push the global counter down to the lower level and then proceed as normal, with one caveat:
	 * - if there are single files right below the root AND we are self-counting,
	 * 	 we should unsee those events with the global counter instead of with the local one.
	 */
	private static Pair<Double, Integer> testDeep(File testDir, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter,
			Model global, int cacheSize, boolean self, int depth) {
		if (testDir.list().length == 1) {
			File only = new File(testDir, testDir.list()[0]);
			if (!only.isFile()) {
				return testDeep(only, vocabulary, tokenizer, counter, global, cacheSize, self, depth);
			} else if (self && depth == 0) {
				System.out.println("Can't self-test on single file, exiting");
				return Pair.of(0.0, 0);
			}
		}
		List<File> files = Util.getFiles(testDir);
		Counter c = Counter.standard();
		Model local = Model.standard(c);
		Model mix = global;
		// Never un-count events at root (if self-testing, obvious, else counter is empty)
		if (depth > 0) {
			files.stream().forEach(file -> CounterRunner.count(file, counter, tokenizer, vocabulary, false));
		}
		// Populate the local counter for deeper packages as well as for the top one if not self-testing
		if (!self || depth > 0) {
			files.stream().forEach(file -> CounterRunner.count(file, c, tokenizer, vocabulary, true));
			mix = new MixModel(global, local);
		}
		double entropy = 0;
		int count = 0;
		for (String child : testDir.list()) {
			if (self && depth == 0) vocabulary.setCheckpoint();
			File file = new File(testDir, child);
			if (file.isFile()) {
				CacheModel cache = cacheSize > 0 ? new CacheModel(cacheSize) : new CacheModel(0);
				Model mixed = new MixModel(mix, cache);
				// Unsee file with local counter unless we are both self-testing and at the root (local counter is empty)
				if (self && depth == 0) CounterRunner.count(file, counter, tokenizer, vocabulary, false);
				else CounterRunner.count(file, c, tokenizer, vocabulary, false);
				DoubleSummaryStatistics stats = ModelRunner.model(file, mixed, tokenizer, vocabulary);
				entropy += stats.getSum();
				count += stats.getCount();
				// Same for re-seeing file
				if (self && depth == 0) CounterRunner.count(file, counter, tokenizer, vocabulary, true);
				else CounterRunner.count(file, c, tokenizer, vocabulary, true);
			}
			else {
				// Pass down local counter unless we are self-testing and at the root (local counter is empty)
				Pair<Double, Integer> lower = testDeep(file, vocabulary, tokenizer, (self && depth == 0 ? counter : c), mix, cacheSize, self, depth + 1);
				entropy += lower.left;
				count += lower.right;
			}
			if (self && depth == 0) vocabulary.restoreCheckpoint();
		}
		if (depth > 0) files.stream().forEach(file -> CounterRunner.count(file, counter, tokenizer, vocabulary, true));
		if (depth == 0) {
			System.out.println("Result: " + entropy / count + "\t" + count);
		}
		else if (depth == 1) {
			System.out.println("At file " + testDir + ":\t" + entropy / count + "\t" + count);
		}
		return Pair.of(entropy, count);
	}
	
	private static int[] predictDeep(File testDir, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter,
			Model global, int cacheSize, boolean self, int depth) {
		if (testDir.list().length == 1) {
			File only = new File(testDir, testDir.list()[0]);
			if (!only.isFile()) {
				return predictDeep(only, vocabulary, tokenizer, counter, global, cacheSize, self, depth);
			} else if (self && depth == 0) {
				System.out.println("Can't self-test on single file, exiting");
				return new int[11];
			}
		}
		List<File> files = Util.getFiles(testDir);
		Counter c = Counter.standard();
		Model local = Model.standard(c);
		Model mix = global;
		// Never un-count events at root (if self-testing, obvious, else counter is empty)
		if (depth > 0) {
			files.stream().forEach(file -> CounterRunner.count(file, counter, tokenizer, vocabulary, false));
		}
		// Populate the local counter for deeper packages as well as for the top one if not self-testing
		if (!self || depth > 0) {
			files.stream().forEach(file -> CounterRunner.count(file, c, tokenizer, vocabulary, true));
			mix = new MixModel(global, local);
		}
		int[] stats = new int[11];
		for (String child : testDir.list()) {
			if (self && depth == 0) vocabulary.setCheckpoint();
			File file = new File(testDir, child);
			if (file.isFile()) {
				CacheModel cache = cacheSize > 0 ? new CacheModel(cacheSize) : new CacheModel(0);
				Model mixed = new MixModel(mix, cache);
				// Unsee file with local counter unless we are both self-testing and at the root (local counter is empty)
				if (self && depth == 0) CounterRunner.count(file, counter, tokenizer, vocabulary, false);
				else CounterRunner.count(file, c, tokenizer, vocabulary, false);
				int[] sts = ModelRunner.predict(file, PREDICTION_CUTOFF, mixed, tokenizer, vocabulary);
				for (int i = 0; i < sts.length; i++) stats[i] += sts[i];
				// Same for re-seeing file
				if (self && depth == 0) CounterRunner.count(file, counter, tokenizer, vocabulary, true);
				else CounterRunner.count(file, c, tokenizer, vocabulary, true);
			}
			else {
				// Pass down local counter unless we are self-testing and at the root (local counter is empty)
				int[] lower = predictDeep(file, vocabulary, tokenizer, (self && depth == 0 ? counter : c), mix, cacheSize, self, depth + 1);
				for (int i = 0; i < lower.length; i++) stats[i] += lower[i];
			
			}
			if (self && depth == 0) vocabulary.restoreCheckpoint();
		}
		if (depth > 0) files.stream().forEach(file -> CounterRunner.count(file, counter, tokenizer, vocabulary, true));
		if (depth <= 1) {
			int sum = Arrays.stream(stats).sum();
			double partial = 0.0;
			if (depth == 1) System.out.print("At file " + testDir + ":\t");
			else System.out.print("Result:\t");
			for (int i = 1; i < stats.length; i++) {
				partial += stats[i];
				System.out.printf("%.4f\t", partial / sum);
			}
			DoubleSummaryStatistics mrr = ModelRunner.getMRR(stats);
			System.out.printf("%.4f\n", mrr.getSum() / mrr.getCount());
		}
		return stats;
	}

	private static void testSimple(File testDir, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter,
			Model model, int cacheSize, boolean self) {
		List<File> files = Util.getFiles(testDir);
		double entropy = 0.0;
		int count = 0;
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			if (self) CounterRunner.count(file, counter, tokenizer, vocabulary, false);
			CacheModel cache = cacheSize > 0 ? new CacheModel(cacheSize) : new CacheModel(0);
			Model mix = new MixModel(model, cache);
			DoubleSummaryStatistics stats = ModelRunner.model(file, mix, vocabulary);
			entropy += stats.getSum();
			count += stats.getCount();
			if (self) CounterRunner.count(file, counter, tokenizer, vocabulary, true);
			if (files.size() < 100 || (i + 1) % 100 == 0) {
				System.out.println("At file " + (i + 1) + ":\t" + entropy / count + "\t" + count);
			}
		}
		System.out.println("Result: " + entropy / count + "\t" + count);
	}

	private static void predictSimple(File testDir, Vocabulary vocabulary, Tokenizer tokenizer, Counter counter,
			Model model, int cacheSize, boolean self) {
		List<File> files = Util.getFiles(testDir);
		int[] stats = new int[11];
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			if (self) CounterRunner.count(file, counter, tokenizer, vocabulary, false);
			CacheModel cache = cacheSize > 0 ? new CacheModel(cacheSize) : new CacheModel(0);
			Model mix = new MixModel(model, cache);
			int[] sts = ModelRunner.predict(file, PREDICTION_CUTOFF, mix, tokenizer, vocabulary);
			
			for (int j = 0; j < sts.length; j++) stats[j] += sts[j];
			if (self) CounterRunner.count(file, counter, tokenizer, vocabulary, true);
			if (files.size() < 100 || (i + 1) % 100 == 0) {
				int sum = Arrays.stream(stats).sum();
				double partial = 0.0;
				System.out.print("At file " + (i + 1) + ":\t");
				for (int j = 1; j < stats.length; j++) {
					partial += stats[j];
					System.out.printf("%.4f\t", partial / sum);
				}
				DoubleSummaryStatistics mrr = ModelRunner.getMRR(stats);
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
		DoubleSummaryStatistics mrr = ModelRunner.getMRR(stats);
		System.out.printf("%.4f\n", mrr.getSum() / mrr.getCount());
	}
}
