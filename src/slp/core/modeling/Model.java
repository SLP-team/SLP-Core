package slp.core.modeling;

import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public interface Model {

	public default Stream<Double> modelStream(Stream<Stream<Integer>> in) {
		return in.map(this::model);
	}

	public double model(Stream<Integer> in);
	
	/**
	 * Returns a pair containing both the probability assigned to this sequence and the
	 * self-reported confidence in this probability
	 * @param in
	 * @return
	 */
	// TODO: make mandatory
	public default Pair<Double, Double> modelWithConfidence(Stream<Integer> in) {
		return Pair.of(model(in), 1.0);
	}
	
	public static Model standard(Counter counter) {
		return new SimpleModel(counter);
	}
}
