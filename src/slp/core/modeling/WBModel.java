package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class WBModel extends NGramModel {

	public WBModel(Counter counter) {
		super(counter);
	}
	
	@Override
	public Pair<Double, Double> modelWithConfidence(List<Integer> in) {
		int[] counts = this.counter.getShortCounts(in);
		int count = counts[0];
		int contextCount = counts[1];
		if (contextCount == 0) return Pair.of(0.0, 0.0);
		
		// Parameters for discount weight
		int[] distinctContext = this.counter.getDistinctCounts(1, in.subList(0, in.size() - 1));
		int N1Plus = distinctContext[0];
		
		// Probability calculation
		double MLE = count / (double) contextCount;
		double lambda = ((double) contextCount) / ((double) N1Plus + contextCount);
		return Pair.of(MLE, lambda);
	}
}
