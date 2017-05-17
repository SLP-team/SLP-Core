package slp.core.modeling.ngram;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class JMModel extends NGramModel {

	private final double lambda;

	public JMModel() {
		this(0.5);
	}
	
	public JMModel(Counter counter) {
		super(counter);
		this.lambda = 0.5;
	}

	public JMModel(double lambda) {
		super();
		this.lambda = lambda;
	}

	@Override
	protected Pair<Double, Double> modelWithConfidence(List<Integer> in, long[] counts) {
		long count = counts[0];
		long contextCount = counts[1];
		
		// Probability calculation
		double MLE = count / (double) contextCount;
		return Pair.of(MLE, this.lambda);
	}
}
