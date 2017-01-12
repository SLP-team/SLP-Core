package slp.core.modeling;

import java.util.Collections;
import java.util.List;

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
	public double model(List<Integer> in) {
		double probability = 0.0;
		double mass = 1.0;
		for (int i = 0; i < in.size(); i++) {
			Pair<Double, Double> prediction = modelWithConfidence(in.subList(i, in.size()));
			double prob = prediction.left;
			double lambda = prediction.right;
			probability += mass * lambda * prob;
			mass *= (1 - lambda);
		}
		probability += mass * this.global.model(Collections.emptyList());
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
		// Mix confidences by same way
		// Alternatively:
		// - independent union estimate:globalConf + localConf - globalConf * localConf;
		// - average: localConf > 0 ? (globalConf + localConf) / 2 : globalConf;
		double mixConf = localConf * localConf + (1 - localConf) * globalConf;
		return Pair.of(mixProb, mixConf);
	}

	@SuppressWarnings("unused")
	private double modelAlt1(List<Integer> in) {
		double l = 0.0;
		double probabilityL = 0.0;
		double probabilityR = 0.0;
		double mass = 1.0;
		for (int i = 0; i < in.size(); i++) {
			Pair<Double, Double> right = this.local.modelWithConfidence(in.subList(i, in.size()));
			if (i == 0) l = right.right;
			double localConf = l > 0 ? l : right.right;
			double localProb = right.left;
			probabilityL += mass * localConf * localProb;
			mass *= (1 - localConf);
		}
		probabilityL += mass * this.local.model(Collections.emptyList());
		
		mass = 1.0;
		for (int i = 0; i < in.size(); i++) {
			Pair<Double, Double> left = this.global.modelWithConfidence(in.subList(i, in.size()));
			double globalProb = left.left;
			double globalConf = left.right;
			probabilityR += mass * globalConf * globalProb;
			mass *= (1 - globalConf);
		}
		probabilityR += mass * this.global.model(Collections.emptyList());
		
		double probability = probabilityL * l + (1 - l) * probabilityR;
		return probability;
	}
	
	@SuppressWarnings("unused")
	private double modelAlt2(List<Integer> in) {
		double l = 0.0;
		double probability = 0.0;
		double mass = 1.0;
		for (int i = 0; i < in.size(); i++) {
			// TEMP
			Pair<Double, Double> left = this.global.modelWithConfidence(in.subList(i, in.size()));
			Pair<Double, Double> right = this.local.modelWithConfidence(in.subList(i, in.size()));
			if (i == 0) l = right.right;
			// Interpolate according to the right (local) model's confidence, upgrade confidence to union
			double globalProb = left.left;
			double globalConf = left.right;
			double localConf = l > 0 ? l : right.right;
			double localProb = right.left;
			double mixProb = localConf * localProb + (1 - localConf) * globalProb;
			double mixConf = localConf > 0 ? (globalConf + localConf) / 2 : globalConf;
			Pair<Double, Double> prediction = Pair.of(mixProb, mixConf);
			// END
//			Pair<Double, Double> prediction = modelWithConfidence(in.subList(i, in.size()));
			double prob = prediction.left;
			double lambda = prediction.right;
			probability += mass * lambda * prob;
			mass *= (1 - lambda);
		}
		probability += mass * this.global.model(Collections.emptyList());
		return probability;
	}
}
