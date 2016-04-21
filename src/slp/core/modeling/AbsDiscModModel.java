package slp.core.modeling;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;

public class AbsDiscModModel implements Model {

	private final Counter counter;

	public AbsDiscModModel(Counter counter) {
		this.counter = counter;
	}
	
	@Override
	public double modelSequence(List<Integer> indices) {
		int[][] counts = this.counter.getFullCounts(indices);
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
			int n3 = this.counter.getNCount(i + 1, 3);
			int n4 = this.counter.getNCount(i + 1, 4);
			double Y = (double) n1 / ((double) n1 + 2*n2);
			double[] Ds = new double[] {
				Y,
				2 - 3*Y*n3/n2,
				3 - 4*Y*n4/n3
			};
			int[] Ns = this.counter.getDistinctCounts(3, indices.subList(indices.size() - i - 1, indices.size() - 1));
			
			// Probability calculation
			double discount = count > 0 ? Ds[Math.min(count, Ds.length) - 1] : 0.0;
			double MLE = Math.max(0.0, count - discount) / contextCount;
			double lambda = 1 - (Ds[0] * Ns[0] + Ds[1] * Ns[1] + Ds[2] * Ns[2]) / contextCount;
			probability += mass * MLE;
			mass *= (1 - lambda);
		}
		probability += mass / Vocabulary.size;
		return probability;
	}

}
