package slp.core.modeling.ngram;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class WBModel extends NGramModel {

	public WBModel() {
		super();
	}
	
	public WBModel(Counter counter) {
		super(counter);
	}
	
	@Override
	protected Pair<Double, Double> modelWithConfidence(List<Integer> in, long[] counts) {
		long count = counts[0];
		long contextCount = counts[1];
		
		// Parameters for discount weight
		int[] distinctContext = this.counter.getDistinctCounts(1, in.subList(0, in.size() - 1));
		int N1Plus = distinctContext[0];
		
		// Probability calculation
		double MLE = count / (double) contextCount;
		double lambda = ((double) contextCount) / ((double) N1Plus + contextCount);
		return Pair.of(MLE, lambda);
	}
}
