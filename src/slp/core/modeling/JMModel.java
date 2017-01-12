package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class JMModel extends NGramModel {

	private final double LAMBDA;

	public JMModel(Counter counter) {
		this(counter, 0.5);
	}
	
	public JMModel(Counter counter, double lambda) {
		super(counter);
		this.LAMBDA = lambda;
	}

	@Override
	public Pair<Double, Double> modelWithConfidence(List<Integer> in) {
		int[] counts = this.counter.getShortCounts(in);
		int count = counts[0];
		int contextCount = counts[1];
		if (contextCount == 0) return Pair.of(0.0, 0.0);
		
		// Probability calculation
		double MLE = count / (double) contextCount;
		return Pair.of(MLE, LAMBDA);
	}
}
