package slp.core.modeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

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
		if (in.isEmpty()) return this.global.model(in);
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

	@Override
	public List<Integer> predict(List<Integer> in, int limit) {
		Map<Integer, List<Pair<Double, Double>>> predictions = new HashMap<>();
		for (int i = 0; i <= in.size(); i++) {
			Map<Integer, List<Pair<Double, Double>>> subs = predictWithConfidence(in.subList(i, in.size()), predictions.keySet(), limit);
			if (subs == null) continue;
			predictions.putAll(subs);
			if (predictions.size() >= limit + 2) break;
		}
		CacheModel.open = false;
		Map<Integer, Double> fullProbs = predictions.entrySet().stream()
			.collect(Collectors.toMap(e -> e.getKey(), e -> {
				ArrayList<Integer> temp = new ArrayList<>(in);
				temp.add(e.getKey());
				return model(temp);
			}));
		CacheModel.open = true;
		List<Entry<Integer, Double>> ranked = fullProbs.entrySet().stream()
			.sorted((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()))
			.limit(limit).collect(Collectors.toList());
		return ranked.stream()
			.map(e -> e.getKey())
			.collect(Collectors.toList());
	}

	@Override
	protected Map<Integer, List<Pair<Double, Double>>> predictWithConfidence(List<Integer> in, Set<Integer> dejavu, int limit) {
		Map<Integer, List<Pair<Double, Double>>> loc = this.local.predictWithConfidence(in, dejavu, limit);
		Map<Integer, List<Pair<Double, Double>>> glob = this.global.predictWithConfidence(in, dejavu, limit);
		if (loc == null) return glob;
		else if (glob == null) return loc;
		Set<Integer> ixs = new HashSet<>(loc.keySet());
		ixs.addAll(glob.keySet());
		Map<Integer, List<Pair<Double, Double>>> fullProbs = ixs.stream()
			.collect(Collectors.toMap(i -> i, i -> {
				List<Pair<Double, Double>> l = loc.get(i);
				List<Pair<Double, Double>> g = glob.get(i);
				List<Pair<Double, Double>> mix = new ArrayList<>();
				if (l == null) return g;
				else if (g == null) return l;
				for (int j = Math.max(l.size(), g.size()) - 1; j >= 0; j--) {
					Pair<Double, Double> gp = j >= g.size() ? Pair.of(0.0, 0.0) : g.get(j);
					Pair<Double, Double> lp = j >= l.size() ? Pair.of(0.0, 0.0) : l.get(j);
					mix.add(0, mix(gp, lp));
				}
				return mix;
			}));
		return fullProbs;
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
		// Mix confidences the same way
		// Alternatively:
		// - independent union estimate: globalConf + localConf - globalConf * localConf;
		// - average: localConf > 0 ? (globalConf + localConf) / 2 : globalConf;
		double mixConf = localConf * localConf + (1 - localConf) * globalConf;
		return Pair.of(mixProb, mixConf);
	}
}
