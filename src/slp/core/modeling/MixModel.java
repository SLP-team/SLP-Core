package slp.core.modeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import slp.core.util.Configuration;
import slp.core.util.Pair;

public class MixModel extends Model {
	
	final Model global;
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
		return mix(left, right);
	}

	private Pair<Double, Double> mix(Pair<Double, Double> left, Pair<Double, Double> right) {
		// Interpolate according to the right (local) model's confidence, upgrade confidence to union
		double globalProb = left.left;
		double globalConf = left.right;
		if (globalConf == 0.0) return right;
		double localProb = right.left;
		double localConf = right.right;
		if (localConf == 0.0) return left;
		double mixProb = localConf * localProb + (1 - localConf) * globalProb;
		// Mix confidences by same way
		// Alternatively:
		// - independent union estimate: globalConf + localConf - globalConf * localConf;
		// - average: localConf > 0 ? (globalConf + localConf) / 2 : globalConf;
		double mixConf = localConf * localConf + (1 - localConf) * globalConf;
		return Pair.of(mixProb, mixConf);
	}
	
	@Override
	public List<Integer> predict(List<Integer> in, int limit) {
//		if (true) return predictAlt1(in, limit);
		Map<Integer, List<Pair<Double, Double>>> predictions = new HashMap<>();
		int start = in.size() < Configuration.order() ? 0 : 1;
		for (int i = start; i < in.size(); i++) {
			Map<Integer, List<Pair<Double, Double>>> subs = predictWithConfidence(in.subList(i, in.size()), limit);
			if (subs == null) continue;
			for (Integer key : subs.keySet()) {
				if (!predictions.containsKey(key)) {
					predictions.put(key, subs.get(key));
				}
			}
		}
		Map<Integer, Double> fullProbs = predictions.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> toProbability(e.getValue())));
		return fullProbs.entrySet().stream()
			.sorted((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()))
			.limit(limit)
			.map(e -> e.getKey())
			.collect(Collectors.toList());
	}

	@Override
	protected Map<Integer, List<Pair<Double, Double>>> predictWithConfidence(List<Integer> in, int limit) {
		Map<Integer, List<Pair<Double, Double>>> loc = this.local.predictWithConfidence(in, limit);
		Map<Integer, List<Pair<Double, Double>>> glob = this.global.predictWithConfidence(in, limit);
		if (loc == null) return glob;
		else if (glob == null) return loc;
		Set<Integer> ixs = new HashSet<>(loc.keySet());
		ixs.addAll(glob.keySet());
		Map<Integer, List<Pair<Double, Double>>> fullProbs = ixs.stream()
			.collect(Collectors.toMap(i -> i, i -> {
				List<Pair<Double, Double>> l = loc.get(i);
				List<Pair<Double, Double>> g = glob.get(i);
				List<Pair<Double, Double>> mix = new ArrayList<>();
				if (l == null) l = new ArrayList<>();
				else if (g == null) g = new ArrayList<>();
				for (int j = Math.max(l.size(), g.size()) - 1; j >= 0; j--) {
					Pair<Double, Double> gp = j >= g.size() ? Pair.of(0.0, 0.0) : g.get(j);
					Pair<Double, Double> lp = j >= l.size() ? Pair.of(0.0, 0.0) : l.get(j);
					mix.add(0, mix(gp, lp));
				}
				return mix;
			}));
		return fullProbs;
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

	@SuppressWarnings("unused")
	private List<Integer> predictAlt1(List<Integer> in, int limit) {
		Map<Integer, List<Pair<Double, Double>>> lpredictions = new HashMap<>();
		Map<Integer, List<Pair<Double, Double>>> gpredictions = new HashMap<>();
		int start = in.size() < Configuration.order() ? 0 : 1;
		for (int i = start; i < in.size(); i++) {
			Map<Integer, List<Pair<Double, Double>>> lsubs = this.local.predictWithConfidence(in.subList(i, in.size()), limit);
			Map<Integer, List<Pair<Double, Double>>> gsubs = this.global.predictWithConfidence(in.subList(i, in.size()), limit);
			if (lsubs != null) {
				for (Integer key : lsubs.keySet()) {
					if (!lpredictions.containsKey(key)) {
						lpredictions.put(key, lsubs.get(key));
					}
				}
			}
			if (gsubs != null) {
				for (Integer key : gsubs.keySet()) {
					if (!gpredictions.containsKey(key)) {
						gpredictions.put(key, gsubs.get(key));
					}
				}
			}
		}
		Map<Integer, Pair<Double, Double>> locals = lpredictions.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> {
			List<Pair<Double, Double>> value = e.getValue();
			double conf = 0.0;
			for (int i = value.size() - 1; i >= 0; i--) {
				Pair<Double, Double> p = value.get(i);
				if (p.right == 0) continue;
				conf = p.right;
				break;
			}
			double prob = toProbability(value);
			return Pair.of(prob, conf);
		}));
		Map<Integer, Pair<Double, Double>> globals = gpredictions.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> {
			List<Pair<Double, Double>> value = e.getValue();
			double conf = 0.0;
			for (int i = value.size() - 1; i >= 0; i--) {
				Pair<Double, Double> p = value.get(i);
				if (p.right == 0) continue;
				conf = p.right;
				break;
			}
			double prob = toProbability(value);
			return Pair.of(prob, conf);
		}));
		Set<Integer> ixs = new HashSet<>(locals.keySet());
		ixs.addAll(globals.keySet());
		Map<Integer, Double> fullProbs = ixs.stream()
				.collect(Collectors.toMap(i -> i, i -> {
					Pair<Double, Double> l = locals.getOrDefault(i, Pair.of(0.0, 0.0));
					Pair<Double, Double> g = globals.getOrDefault(i, Pair.of(0.0, 0.0));
					double globalProb = g.left;
					double localProb = l.left;
					double localConf = l.right;
					double mixProb = localConf * localProb + (1 - localConf) * globalProb;
					return mixProb;
				}));
		return fullProbs.entrySet().stream()
				.sorted((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()))
				.limit(limit)
				.map(e -> e.getKey())
				.collect(Collectors.toList());
	}
}
