package slp.core.modeling;

import java.util.List;

import slp.core.counting.Vocabulary;
import slp.core.util.Pair;

public class MixModel extends Model {
	
	private final Model global;
	private final Model local;

	public MixModel(Model global) {
		this(global, new CacheModel());
	}

	public MixModel(Model first, Model second) {
		this.global = first;
		this.local = second;
	}

	@Override
	public double modelSequence(List<Integer> in) {
		double probability = 0.0;
		double mass = 1.0;
		for (int i = 0; i < in.size(); i++) {
			Pair<Double, Double> prediction = modelWithConfidence(in.subList(i, in.size()));
			double prob = prediction.left;
			double lambda = prediction.right;
			probability += mass * lambda * prob;
			mass *= (1 - lambda);
		}
		probability += mass / Vocabulary.size;
		return probability;
	}

	@Override
	public Pair<Double, Double> modelWithConfidence(List<Integer> in) {
		Pair<Double, Double> left = this.global.modelWithConfidence(in);
		Pair<Double, Double> right = this.local.modelWithConfidence(in);
		// Interpolate according to the right (local) model's confidence, upgrade confidence to union
		double globalProb = left.left;
		double globalConf = left.right;
		double localConf = right.right;
		double localProb = right.left;
		double mixProb = localConf * localProb + (1 - localConf) * globalProb;
		double mixConf = (localConf  + globalConf) / 2;// Temp just avg, indep. union: globalConf + localConf - globalConf * localConf;
		return Pair.of(mixProb, mixConf);
	}

}
