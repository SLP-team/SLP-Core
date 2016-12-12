package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.util.Pair;

public abstract class NGramModel extends Model {

	final Counter counter;

	public NGramModel(Counter counter) {
		this.counter = counter;
	}
	
	public final double modelSequence(List<Integer> in) {
		double probability = 0.0;
		double mass = 1.0;
		for (int i = 0; i < in.size(); i++) {
			Pair<Double, Double> prediction = modelWithConfidence(in.subList(i, in.size()));
			double prob = prediction.left;
			double lambda = prediction.right;
			probability += mass * lambda * prob;
			mass *= (1 - lambda);
		}
		probability += mass / Vocabulary.size;
		return probability;
	}
}
