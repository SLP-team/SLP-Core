package slp.core.modeling;

import java.util.stream.Stream;

import slp.core.counting.Counter;

public class SimpleModel implements Model {

	private final Counter counter;
	
	public SimpleModel(Counter counter) {
		this.counter = counter;
	}
	
	@Override
	public Stream<Double> model(Stream<Integer[]> in) {
		return in.map(this::model);
	}

	@Override
	public double model(Integer[] in) {
		int[][] counts = this.counter.counts().getFullCounts(in);
		double probability = 0.0;
		double mass = 1.0;
		double lambda = 0.5;
		for (int i = in.length - 1; i >= 0; i--) {
			int count = counts[0][i];
			int contextCount = counts[1][i];
			if (contextCount == 0) continue;
			else {
				double MLE = count / (double) contextCount;
				probability += mass * lambda * MLE;
				mass *= (1 - lambda);
			}
		}
		probability += mass / this.counter.counts().getOverallDistinct();
		return probability;
	}

}
