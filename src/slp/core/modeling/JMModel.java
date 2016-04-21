package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;

public class JMModel implements Model {

	private final Counter counter;
	private final double LAMBDA;

	public JMModel(Counter counter) {
		this(counter, 0.5);
	}
	
	public JMModel(Counter counter, double lambda) {
		this.counter = counter;
		this.LAMBDA = lambda;
	}

	@Override
	public double modelSequence(List<Integer> in) {
		int[][] counts = this.counter.getFullCounts(in);
		double probability = 0.0;
		double mass = 1.0;
		for (int i = counts[0].length - 1; i >= 0; i--) {
			// General parameters
			int count = counts[0][i];
			int contextCount = counts[1][i];
			if (contextCount == 0) continue;
			
			// Probability calculation
			double MLE = count / (double) contextCount;
			double lambda = this.LAMBDA;
			probability += mass * lambda * MLE;
			mass *= (1 - lambda);
		}
		probability += mass / Vocabulary.size;
		return probability;
	}

}
