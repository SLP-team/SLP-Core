package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.util.Pair;

public interface Model {
	
	public default double model(List<Integer> in) {
		// Very rare (one in ten million) "sanity check"
		if (Math.random() < 10E-7) {
			optionalSanityCheck(in);
		}
		return this.modelSequence(in);
	}

	double modelSequence(List<Integer> indices);
	
	/**
	 * Returns a pair containing both the probability assigned to this sequence and the
	 * self-reported confidence in this probability
	 * @param in
	 * @return
	 */
	// TODO: make mandatory
	public default Pair<Double, Double> modelWithConfidence(List<Integer> in) {
		return Pair.of(modelSequence(in), 1.0);
	}
	
	default void optionalSanityCheck(List<Integer> indices) {
		int old = indices.get(indices.size() - 1);
		double sum = 0.0;
		for (int i = 0; i < Vocabulary.size; i++) {
			indices.set(indices.size() - 1, i);
			double prob = this.modelSequence(indices);
			sum += prob;
		}
		if (Math.abs(sum - 1.0) > 0.01) {
			System.err.println("Sanity check failed! For sequence " + indices + " (total probability sums to: "+ sum + ")");
		}
		indices.set(indices.size() - 1, old);
	}

	public static Model standard(Counter counter) {
		return new AbsDiscModModel(counter);
	}
}
