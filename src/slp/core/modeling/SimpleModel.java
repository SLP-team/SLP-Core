package slp.core.modeling;

import java.util.stream.Stream;

import slp.core.counting.Counter;

public class SimpleModel implements Model {

	private final Counter counter;
	private final double LAMBDA = 0.5;
	
	public SimpleModel(Counter counter) {
		this.counter = counter;
	}
	
	@Override
	public double model(Stream<Integer> in) {
		int[][] counts = this.counter.getFullCounts(in);
		double probability = 0.0;
		double mass = 1.0;
		for (int i = counts[0].length - 1; i >= 0; i--) {
			int count = counts[0][i];
			int contextCount = counts[1][i];
			if (contextCount == 0) continue;
			else {
				double MLE = count / (double) contextCount;
				probability += mass * this.LAMBDA * MLE;
				mass *= (1 - this.LAMBDA);
			}
		}
		probability += mass / this.counter.getDistinctSuccessors();
		return probability;
	}

}
