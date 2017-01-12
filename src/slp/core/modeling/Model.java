package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public abstract class Model {
	
	public abstract double model(List<Integer> in);

	/**
	 * Returns a pair containing both the probability assigned to this sequence and the
	 * self-reported confidence in this probability
	 */
	abstract Pair<Double, Double> modelWithConfidence(List<Integer> in);
	
	public static Model standard(Counter counter) {
		return new WBModel(counter);
	}
}
