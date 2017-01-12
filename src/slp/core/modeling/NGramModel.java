package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.counting.sequence.LightMapCounter;
import slp.core.util.Pair;

public abstract class NGramModel extends Model {

	protected final Counter counter;

	public NGramModel(Counter counter) {
		this.counter = counter;
	}
	
	@Override
	public double model(List<Integer> in) {
		// Very rare (one in ten million) "sanity check"
		if (!in.isEmpty() && Math.random() < 10E-7 && this.counter instanceof LightMapCounter) {
			optionalSanityCheck(in);
		}
		return modelSequence(in);
	}
	
	public final double modelSequence(List<Integer> in) {
		if (this.counter.getDistinctSuccessors() == 0) return 0;
		double probability = 0.0;
		double mass = 1.0;
		for (int i = 0; i < in.size(); i++) {
			Pair<Double, Double> prediction = modelWithConfidence(in.subList(i, in.size()));
			double prob = prediction.left;
			double lambda = prediction.right;
			probability += mass * lambda * prob;
			mass *= (1 - lambda);
		}
		probability += mass / this.counter.getDistinctSuccessors();
		return probability;
	}
	
	private final void optionalSanityCheck(List<Integer> indices) {
		int old = indices.get(indices.size() - 1);
		double sum = 0.0;
		LightMapCounter l = (LightMapCounter) this.counter;
		int[] succIXs = l.getSuccIXs();
		for (int i : succIXs) {
			if (i == Integer.MAX_VALUE) continue;
			indices.set(indices.size() - 1, i);
			double prob = this.modelSequence(indices);
			sum += prob;
		}
		indices.set(indices.size() - 1, old);
		if (Math.abs(sum - 1.0) > 0.01) {
			System.err.println("Sanity check failed! For sequence " + indices + " (total probability sums to: " + sum + ")");
		}
	}
}
