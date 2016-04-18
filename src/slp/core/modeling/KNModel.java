package slp.core.modeling;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.Counter;

public class KNModel implements Model {

	private final Counter counter;

	public KNModel(Counter counter) {
		this.counter = counter;
	}
	
	@Override
	public double modelSequence(Stream<Integer> in) {
		List<Integer> indices = in.collect(Collectors.toList());
		int[][] counts = this.counter.getFullCounts(indices.stream());
		double probability = 0.0;
		double mass = 1.0;
		// say [a, b, c, d], i ranges from 3 to 0 incl. 
		for (int i = counts[0].length - 1; i >= 0; i--) {
			double D = 0.75;
			int count = counts[0][i];
			int contextCount = counts[1][i];
			if (contextCount == 0) continue;
			int[] distinctFull = this.counter.getDistinctCounts(1, indices.subList(indices.size() - i - 1, indices.size()).stream());
			
		}
		probability += mass / this.counter.getDistinctSuccessors();
		return probability;
	}

}
