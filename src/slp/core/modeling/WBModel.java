package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;

public class WBModel implements Model {

	private final Counter counter;

	public WBModel(Counter counter) {
		this.counter = counter;
	}
	
	@Override
	public double modelSequence(List<Integer> indices) {
		int[][] counts = this.counter.getFullCounts(indices);
		double probability = 0.0;
		double mass = 1.0;
		// say [a], counts will be [c(a), c(*)], i is just 0
		for (int i = counts[0].length - 1; i >= 0; i--) {
			// General parameters
			int count = counts[0][i];
			int contextCount = counts[1][i];
			if (contextCount == 0) continue;
			
			// Parameters for discount weight
			int[] distinctContext = this.counter.getDistinctCounts(1, indices.subList(indices.size() - i - 1, indices.size() - 1));
			int N1Plus = distinctContext[0];
			
			// Probability calculation
			double MLE = count / (double) contextCount;
			double lambda = ((double) contextCount) / ((double) N1Plus + contextCount);
			probability += mass * lambda * MLE;
			mass *= (1 - lambda);
		}
		probability += mass / Vocabulary.size;
		return probability;
	}

}
