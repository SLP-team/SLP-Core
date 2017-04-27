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
		if (sequence.size() == ModelRunner.getNGramOrder() || index == input.size() - 1) {
			this.counter.count(sequence);
		}
	}
	
	@Override
	public void forget(List<Integer> input) {
		this.counter.unCountBatch(NGramSequencer.sequenceForward(input));
	}
	
	@Override
	public void forgetToken(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index);
		if (sequence.size() == ModelRunner.getNGramOrder() || index == input.size() - 1) {
			this.counter.unCount(sequence);
		}
	}

	@Override
	public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index);
		double prob = 0;
		double conf = 0;
		double weight = Math.pow(2, -sequence.size() - 1);
		int hits = 0;
		for (int i = sequence.size() - 1; i >= 0; i--) {
			Pair<Double, Double> resN = this.modelWithConfidence(sequence.subList(i, sequence.size()));
			if (resN.right == 0) break;
			hits++;
			weight *= 2;
			conf += weight;
			prob += resN.left*weight;
		}
		prob /= conf;
		conf = 1.0 - Math.pow(2, -hits);
//		return Pair.of(prob, conf);
		Pair<Double, Double> res = Pair.of(0.0, 0.0);
		for (int i = 0; i < sequence.size(); i++) {
			Pair<Double, Double> resN = this.modelWithConfidence(sequence.subList(i, sequence.size()));
			if (resN.right == 0) continue;
			double probG = res.left + resN.left*resN.right*(1 - res.right);
			double confG = res.right + resN.right - res.right*resN.right;
			res = Pair.of(probG, confG);
		}
		// Normalize probability to sum to 1 (instead of to confidence)
		if (res.right > 0) res = Pair.of(res.left/res.right, res.right);
		if (Math.abs(res.left - prob) > 1e-3 || res.right != conf) System.out.println(sequence + "\t" + res + "\t" + prob + ", " + conf);
		return res;
	}

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
	
	private Pair<Double, Double> prob(List<Integer> input, int index, int prediction) {
		Integer prev = input.set(index, prediction);
		Pair<Double, Double> prob = this.modelToken(input, index);
		input.set(index, prev);
		return prob;
	}

	protected abstract Pair<Double, Double> modelWithConfidence(List<Integer> subList);
	
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
