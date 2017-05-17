package slp.core.modeling.ngram;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import slp.core.counting.Counter;
import slp.core.counting.trie.TrieCounter;
import slp.core.modeling.AbstractModel;
import slp.core.modeling.ModelRunner;
import slp.core.sequencing.NGramSequencer;
import slp.core.util.Pair;

public abstract class NGramModel extends AbstractModel {
	
	protected Counter counter;

	public NGramModel() {
		this(new TrieCounter());
	}
	
	public NGramModel(Counter counter) {
		this.counter = counter;
	}

	public Counter getCounter() {
		return this.counter;
	}

	@Override
	public void notify(File next) { }

	@Override
	public void learn(List<Integer> input) {
		this.counter.countBatch(NGramSequencer.sequenceForward(input));
	}
	
	@Override
	public void learnToken(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index);
		for (int i = 0; i < sequence.size(); i++) this.counter.count(sequence.subList(i, sequence.size()));
	}
	
	@Override
	public void forget(List<Integer> input) {
		this.counter.unCountBatch(NGramSequencer.sequenceForward(input));
	}
	
	@Override
	public void forgetToken(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index);
		for (int i = 0; i < sequence.size(); i++) this.counter.unCount(sequence.subList(i, sequence.size()));
	}

	@Override
	public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index);
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
		probability /= mass;
		// In the new model, final confidence is same for all n-gram models, proportional to longest context seen
		double confidence = (1 - Math.pow(2, -hits));
		return Pair.of(probability, confidence);
	}

	protected abstract Pair<Double, Double> modelWithConfidence(List<Integer> subList, long[] counts);

	@Override
	public Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index);
		Set<Integer> predictions = new HashSet<>();
		int limit = ModelRunner.getPredictionCutoff();
		for (int i = 0; i < sequence.size(); i++) {
			predictions.addAll(this.predictWithConfidence(sequence.subList(i, sequence.size()), limit, predictions));
		}
		return predictions.stream().collect(Collectors.toMap(p -> p, p -> prob(input, index, p)));
	}
	
	private Map<List<Integer>, Pair<Integer, List<Integer>>> mem = new HashMap<>();
	protected final Collection<Integer> predictWithConfidence(List<Integer> indices, int limit, Set<Integer> covered) {
		List<Integer> top;
		int key = 31*(counter.getSuccessorCount() + 31*counter.getCount());
		if (this.mem.containsKey(indices) && this.mem.get(indices).left.equals(key)) {
			top = this.mem.get(indices).right;
		}
		else {
			if (this.mem.containsKey(indices)) this.mem.clear();
			top = this.counter.getTopSuccessors(indices, limit);
			if (this.counter.getSuccessorCount(indices) > 1000) {
				this.mem.put(indices, Pair.of(key, top));
			}
		}
		return top;
	}

	private Pair<Double, Double> prob(List<Integer> input, int index, int prediction) {
		Integer prev = input.set(index, prediction);
		Pair<Double, Double> prob = this.modelToken(input, index);
		input.set(index, prev);
		return prob;
	}

	private static Class<? extends NGramModel> standard = JMModel.class;
	public static void setStandard(Class<? extends NGramModel> clazz) {
		standard = clazz;
	}
	public static NGramModel standard() {
		try {
			return standard.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
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
}
