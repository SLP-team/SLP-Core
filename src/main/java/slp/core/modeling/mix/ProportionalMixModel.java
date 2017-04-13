package slp.core.modeling.mix;

import java.util.List;

import slp.core.modeling.Model;
import slp.core.util.Pair;

public class ProportionalMixModel extends MixModel {

	public ProportionalMixModel(Model model1, Model model2) {
		super(model1, model2);
	}

	@Override
	protected Pair<Double, Double> mix(List<Integer> input, int index, Pair<Double, Double> res1, Pair<Double, Double> res2) {
		if (res1.right.equals(0.0)) return res2;
		else if (res2.right.equals(0.0)) return res1;
		double confidence = Math.max(res1.right, res2.right);
		double probability = (res1.left*res1.right + res2.left*res2.right)/(res1.right + res2.right);
		return Pair.of(probability, confidence);
	}
}
