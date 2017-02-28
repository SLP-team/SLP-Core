package slp.core.modeling;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import slp.core.counting.Counter;
import slp.core.modeling.ngram.NGramModel;
import slp.core.modeling.ngram.WBModel;
import slp.core.util.Pair;

public abstract class Model {
	
	public abstract double model(List<Integer> in);

	/**
	 * Returns a pair containing both the probability assigned to this sequence and the
	 * self-reported confidence in this probability
	 */
	protected abstract Pair<Double, Double> modelWithConfidence(List<Integer> in);

	public List<Integer> predict(List<Integer> in) {
		return predict(in, 10);
	}
	public abstract List<Integer> predict(List<Integer> in, int limit);

	protected Map<Integer, List<Pair<Double, Double>>> predictWithConfidence(List<Integer> in, int limit) {
		return predictWithConfidence(in, new HashSet<>(), limit);
	}
	protected abstract Map<Integer, List<Pair<Double, Double>>> predictWithConfidence(List<Integer> in, Set<Integer> dejavu, int limit);
	
	protected double toProbability(List<Pair<Double, Double>> values) {
		double probability = 0.0;
		double mass = 1.0;
		for (int i = values.size() - 1; i >= 0; i--) {
			Pair<Double, Double> prediction = values.get(i);
			double prob = prediction.left;
			double lambda = prediction.right;
			probability += mass * lambda * prob;
			mass *= (1 - lambda);
		}
		probability += mass * this.model(Collections.emptyList());
		return probability;
	}

	private static Class<? extends NGramModel> standard = WBModel.class;
	
	public static void setStandard(Class<? extends NGramModel> clazz) {
		standard = clazz;
	}
	
	public static Model standard(Counter counter) {
		try {
			return standard.getConstructor(Counter.class).newInstance(counter);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			return new WBModel(counter);
		}
	}
}
