package slp.core.modeling.ngram;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import slp.core.counting.Counter;
import slp.core.counting.trie.MapTrieCounter;
import slp.core.modeling.AbstractModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.sequencing.NGramSequencer;
import slp.core.util.Pair;

public abstract class NGramModel extends AbstractModel {
	
	protected final int order;
	public Counter counter;

	public NGramModel() {
		this(ModelRunner.DEFAULT_NGRAM_ORDER);
	}
	
	public NGramModel(Counter counter) {
		this(ModelRunner.DEFAULT_NGRAM_ORDER, counter);
	}

	public NGramModel(int order) {
		this(order, new MapTrieCounter());
	}
	
	public NGramModel(int order, Counter counter) {
		this.order = order;
		this.counter = counter;
	}

	public Counter getCounter() {
		return this.counter;
	}

	@Override
	public void notify(File next) { }

	@Override
	public void learn(List<Integer> input) {
		this.counter.countBatch(NGramSequencer.sequenceForward(input, this.order));
	}
	
	@Override
	public void learnToken(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index, this.order);
		for (int i = 0; i < sequence.size(); i++) {
			this.counter.count(sequence.subList(i, sequence.size()));
		}
	}
	
	@Override
	public void forget(List<Integer> input) {
		this.counter.unCountBatch(NGramSequencer.sequenceForward(input, this.order));
	}
	
	@Override
	public void forgetToken(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index, this.order);
		for (int i = 0; i < sequence.size(); i++) {
			this.counter.unCount(sequence.subList(i, sequence.size()));
		}
	}

	@Override
	public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index, this.order);
		double probability = 0.0;
		double mass = 0.0;
		int hits = 0;
		for (int i = sequence.size() - 1; i >= 0; i--) {
			List<Integer> sub = sequence.subList(i, sequence.size());
			long[] counts = this.counter.getCounts(sub);
			if (counts[1] == 0) break;
			Pair<Double, Double> resN = this.modelWithConfidence(sub, counts);
			double prob = resN.left;
			double conf = resN.right;
			mass = (1 - conf)*mass + conf;
			probability = (1 - conf)*probability + conf*prob;
			hits++;
		}
		if (mass > 0) probability /= mass;
		// In the new model, final confidence is asymptotically close to 1 for all n-gram models
		double confidence = 1 - Math.pow(2, -hits);
		return Pair.of(probability, confidence);
	}

	protected abstract Pair<Double, Double> modelWithConfidence(List<Integer> subList, long[] counts);

	@Override
	public Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index - 1, this.order);
		Set<Integer> predictions = new HashSet<>();
		for (int i = 0; i <= sequence.size(); i++) {
			int limit = ModelRunner.GLOBAL_PREDICTION_CUTOFF - predictions.size();
			if (limit <= 0) break;
			predictions.addAll(this.counter.getTopSuccessors(sequence.subList(i, sequence.size()), limit));
		}
		return predictions.stream().collect(Collectors.toMap(p -> p, p -> prob(input, index, p)));
	}
	
	private Pair<Double, Double> prob(List<Integer> input, int index, int prediction) {
		boolean added = index == input.size();
		if (added) input.add(0);
		int prev = input.set(index, prediction);
		Pair<Double, Double> prob = this.modelAtIndex(input, index);
		if (added) input.remove(input.size() - 1);
		else input.set(index, prev);
		return prob;
	}

	private static Class<? extends NGramModel> standard = JMModel.class;
	public static void setStandard(Class<? extends NGramModel> clazz) {
		standard = clazz;
	}
	public static NGramModel standard() {
		try {
			return standard.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| SecurityException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
			return new JMModel();
		}
	}
	public static NGramModel standard(Counter counter) {
		try {
			return standard.getConstructor(Counter.class).newInstance(counter);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException
				| InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
			return new JMModel();
		}
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
