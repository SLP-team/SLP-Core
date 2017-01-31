package slp.core.prediction;

import java.util.List;

import slp.core.counting.sequence.LightMapCounter;

public interface Predictor {

	public abstract List<Integer> predict(List<Integer> in, int limit);

	public static Predictor standard(LightMapCounter counter) {
		return new SimplePredictor(counter);
	}
}
