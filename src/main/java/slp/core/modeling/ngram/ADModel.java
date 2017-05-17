package slp.core.modeling.ngram;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class ADModel extends NGramModel {
	
	public ADModel() {
		super();
	}
	
	public ADModel(Counter counter) {
		super(counter);
	}
	
	@Override
	protected Pair<Double, Double> modelWithConfidence(List<Integer> in, long[] counts) {
		long count = counts[0];
		long contextCount = counts[1];

		// Parameters for discount weight
		int n1 = this.counter.getCountofCount(in.size(), 1);
		int n2 = this.counter.getCountofCount(in.size(), 2);
		double D = (double) n1 / ((double) n1 + 2*n2);
		int[] distinctContext = this.counter.getDistinctCounts(1, in.subList(0, in.size() - 1));
		int N1Plus = distinctContext[0];
		
		// Probability calculation
		double MLEDisc = Math.max(0.0, count - D) / contextCount;
		double lambda = 1 - N1Plus * D / contextCount;
		// Must divide MLE by lambda to match contract
		return Pair.of(MLEDisc/lambda, lambda);
	}
}
