package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class AbsDiscModel extends NGramModel {

	public AbsDiscModel(Counter counter) {
		super(counter);
	}
	
	@Override
	public Pair<Double, Double> modelWithConfidence(List<Integer> in) {
		int[] counts = this.counter.getShortCounts(in);
		int count = counts[0];
		int contextCount = counts[1];
		if (contextCount == 0) return Pair.of(0.0, 0.0);
		
		// Parameters for discount weight
		int n1 = this.counter.getNCount(in.size(), 1);
		int n2 = this.counter.getNCount(in.size(), 2);
		double D = (double) n1 / ((double) n1 + 2*n2);
		int[] distinctContext = this.counter.getDistinctCounts(1, in.subList(0, in.size() - 1));
		int N1Plus = distinctContext[0];
		
		// Probability calculation
		double MLE = Math.max(0.0, count - D) / contextCount;
		double lambda = 1 - N1Plus * D / contextCount;
		// Must pre-divide MLE by lambda to match contract
		return Pair.of(MLE/lambda, lambda);
	}
}
