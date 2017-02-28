package slp.core.modeling.ngram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import slp.core.counting.Counter;
import slp.core.counting.sequence.LightMapCounter;
import slp.core.modeling.Model;
import slp.core.util.Configuration;
import slp.core.util.Pair;

public abstract class NGramModel extends Model {

	protected final Counter counter;

	public NGramModel(Counter counter) {
		this.counter = counter;
	}
	
	@Override
	public double model(List<Integer> in) {
		// Very rare (one in ten million) "sanity check"
		if (!in.isEmpty() && Math.random() < 10E-7 && this.counter instanceof LightMapCounter) {
			optionalSanityCheck(in);
		}
		return modelSequence(in);
	}
	
	public final double modelSequence(List<Integer> in) {
		if (this.counter.getDistinctSuccessors() == 0) return 0;
		if (in.isEmpty()) return 1.0 / this.counter.getDistinctSuccessors();
		List<Pair<Double, Double>> probs = new ArrayList<>();
		for (int i = in.size() - 1; i >= 0; i--) {
			probs.add(modelWithConfidence(in.subList(i, in.size())));
		}
		return toProbability(probs);
	}

	@Override
	public List<Integer> predict(List<Integer> in, int limit) {
		Map<Integer, List<Pair<Double, Double>>> predictions = new HashMap<>();
		int start = in.size() < Configuration.order() ? 0 : 1;
		for (int i = start; i <= in.size(); i++) {
			Map<Integer, List<Pair<Double, Double>>> subs = predictWithConfidence(in.subList(i, in.size()), limit);
			if (subs == null) continue;
			for (Integer key : subs.keySet()) {
				if (!predictions.containsKey(key)) {
					predictions.put(key, subs.get(key));
				}
			}
		}
		Map<Integer, Double> withProbs = predictions.entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, e -> toProbability(e.getValue())));
		return withProbs.entrySet().stream()
			.sorted((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()))
			.limit(limit)
			.map(e -> e.getKey())
			.collect(Collectors.toList());
	}

	private Map<List<Integer>, Pair<Integer, List<Integer>>> mem = new HashMap<>();
	@Override
	protected Map<Integer, List<Pair<Double, Double>>> predictWithConfidence(List<Integer> in, Set<Integer> dejavu, int limit) {
		if (!(this.counter instanceof LightMapCounter)) return null;
		Object node = ((LightMapCounter) this.counter).getNode(in);
		if (node == null) return null;
		if (node instanceof int[]) {
			int[] singleton = (int[]) node;
			if (singleton.length == 1) return null;
			int prediction = singleton[1];
			if (dejavu.contains(prediction)) return null;
			List<Pair<Double, Double>> confs = new ArrayList<>();
			List<Integer> temp = new ArrayList<>(in);
			temp.add(prediction);
			for (int i = temp.size() - 1; i >= 0; i--) {
				confs.add(modelWithConfidence(temp.subList(i, temp.size())));
			}
			return Collections.singletonMap(prediction, confs);
		}
		else {
			LightMapCounter counter = (LightMapCounter) node;
			int[] succIXs = counter.getSuccIXs();
			List<Integer> top;
			int key = 31*(counter.getDistinctSuccessors() + 31*counter.getCount());
			if (this.mem.containsKey(in) && this.mem.get(in).left.equals(key)) {
				top = this.mem.get(in).right;
			}
			else {
				if (this.mem.containsKey(in)) this.mem.clear();
				Map<Integer, Integer> counts = Arrays.stream(succIXs)
					.mapToObj(i -> i)
					.filter(i -> i != Integer.MAX_VALUE)
					.collect(Collectors.toMap(i -> i, i -> counter.getShortCounts(Collections.singletonList(i))[0]));
				if (counts.isEmpty()) return null;
				top = counts.entrySet().stream()
						.sorted((e1, e2) -> -Integer.compare(e1.getValue(), e2.getValue()))
						.limit(limit)
						.map(e -> e.getKey())
						.collect(Collectors.toList());
				if (counts.size() > 1000) {
					this.mem.put(in, Pair.of(key, top));
				}
			}
			Map<Integer, List<Pair<Double, Double>>> tops = new ConcurrentHashMap<>();
			top.stream()
				.filter(ix -> !dejavu.contains(ix))
				.limit(Math.max(1, limit - dejavu.size()))
				.forEach(ix -> {
					List<Pair<Double, Double>> confs = new ArrayList<>();
					List<Integer> temp = new ArrayList<>(in);
					temp.add(ix);
					for (int i = temp.size() - 1; i >= 0; i--) {
						confs.add(modelWithConfidence(temp.subList(i, temp.size())));
					}
					tops.put(ix, confs);
				});
			return tops;
		}
	}

	private final void optionalSanityCheck(List<Integer> indices) {
		int old = indices.get(indices.size() - 1);
		double sum = 0.0;
		LightMapCounter l = (LightMapCounter) this.counter;
		int[] succIXs = l.getSuccIXs();
		for (int i : succIXs) {
			if (i == Integer.MAX_VALUE) continue;
			indices.set(indices.size() - 1, i);
			double prob = this.modelSequence(indices);
			sum += prob;
		}
		indices.set(indices.size() - 1, old);
		if (Math.abs(sum - 1.0) > 0.01) {
			System.err.println("Sanity check failed! For sequence " + indices + " (total probability sums to: " + sum + ")");
		}
	}
}
