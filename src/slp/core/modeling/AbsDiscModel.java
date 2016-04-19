package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;

public class AbsDiscModel implements Model {

	private final Counter counter;

	public AbsDiscModel(Counter counter) {
		this.counter = counter;
	}
	
	@Override
	public double modelSequence(List<Integer> indices) {
		int[][] counts = this.counter.getFullCounts(indices.stream());
		double probability = 0.0;
		double mass = 1.0;
		// say [a, b, c, d], i ranges from 3 to 0 incl.
		for (int i = counts[0].length - 1; i >= 0; i--) {
			// General parameters
			int count = counts[0][i];
			int contextCount = counts[1][i];
			if (contextCount == 0) continue;
			
			// Parameters for discount weight
			int n1 = this.counter.getNCount(i + 1, 1);
			int n2 = this.counter.getNCount(i + 1, 2);
			double D = (double) n1 / ((double) n1 + 2*n2);
			int[] distinctContext = this.counter.getDistinctCounts(1, indices.subList(indices.size() - i - 1, indices.size() - 1).stream());
			int N1Plus = distinctContext[0];
			
			// Probability calculation
			double MLE = Math.max(0.0, count - D) / contextCount;
			double lambda = 1 - N1Plus * D / contextCount;
			probability += mass * MLE;
			mass *= (1 - lambda);
		}
		probability += mass / this.counter.getDistinctSuccessors();
		return probability;
	}

}
