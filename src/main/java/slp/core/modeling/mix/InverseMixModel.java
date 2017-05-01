package slp.core.modeling.mix;

import java.util.List;

import slp.core.modeling.Model;
import slp.core.util.Pair;

public class InverseMixModel extends MixModel {

	public InverseMixModel(Model model1, Model model2) {
		super(model1, model2);
	}

	@Override
	protected Pair<Double, Double> mix(List<Integer> input, int index, Pair<Double, Double> res1, Pair<Double, Double> res2) {
		if (res1.right.equals(0.0) && res2.right.equals(0.0)) {
			return Pair.of(0.0, 0.0);
		}
		else if (res2.right.equals(0.0)) return res1;
		else if (res1.right.equals(0.0)) return res2;
		double lNorm = 1/(1 - res1.right);
		double rNorm = 1/(1 - res2.right);
		if (lNorm > 1000) lNorm = 1000;
		if (rNorm > 1000) rNorm = 1000;
		double probability = (res1.left*lNorm + res2.left*rNorm)/(lNorm + rNorm);
		double confidence = Math.max(res1.right, res2.right);
		return Pair.of(probability, confidence);
	}
}
