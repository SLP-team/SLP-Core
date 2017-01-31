package slp.core.modeling.ngram;

import java.util.List;

import slp.core.counting.Counter;
import slp.core.util.Pair;

public class KNModel extends NGramModel {

	public KNModel(Counter counter) {
		super(counter);
	}

	@Override
	public Pair<Double, Double> modelWithConfidence(List<Integer> in) {
		// TODO, needs improvements of current counter to be possible
		return null;
	}

}
