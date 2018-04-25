package slp.core.modeling.ngram;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class JMModel extends NGramModel {

	private static final double DEFAULT_LAMBDA = 0.5;
	private final double lambda;

	public JMModel() {
		this(DEFAULT_LAMBDA);
	}
	
	public JMModel(double lambda) {
		super();
		this.lambda = lambda;
	}
	
	public JMModel(int order) {
		super(order);
		this.lambda = DEFAULT_LAMBDA;
	}

	public JMModel(Counter counter) {
		super(counter);
		this.lambda = DEFAULT_LAMBDA;
	}

	public JMModel(int order, Counter counter) {
		this(DEFAULT_LAMBDA, order, counter);
	}

	public JMModel(double lambda, int order, Counter counter) {
		super(order, counter);
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
