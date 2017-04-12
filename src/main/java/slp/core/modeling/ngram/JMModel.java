package core.modeling.ngram;

import java.util.List;

import core.counting.Counter;
import core.util.Pair;

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
	protected Pair<Double, Double> modelWithConfidence(List<Integer> in) {
		int[] counts = this.counter.getCounts(in);
		int count = counts[0];
		int contextCount = counts[1];
		if (contextCount == 0) return Pair.of(0.0, 0.0);
		
		// Probability calculation
		double MLE = count / (double) contextCount;
		return Pair.of(MLE, this.lambda);
	}
}
