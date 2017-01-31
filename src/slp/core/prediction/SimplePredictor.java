package slp.core.prediction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import slp.core.counting.sequence.LightMapCounter;

public class SimplePredictor implements Predictor {

	private LightMapCounter counter;

	public SimplePredictor(LightMapCounter counter) {
		this.counter = counter;
	}
	
	@Override
	public List<Integer> predict(List<Integer> in, int limit) {
		Object node = this.counter.getNode(in);
		if (node == null) return null;
		if (node instanceof int[]) {
			int[] singleton = (int[]) node;
			if (singleton.length == 1) return null;
			return Collections.singletonList(singleton[1]);
		}
		else {
			LightMapCounter counter = (LightMapCounter) node;
			int[] succIXs = counter.getSuccIXs();
			Map<Integer, Integer> counts = Arrays.stream(succIXs)
				.mapToObj(i -> i)
				.filter(i -> i != Integer.MAX_VALUE)
				.collect(Collectors.toMap(i -> i, i -> counter.getShortCounts(Collections.singletonList(i))[0]));
			List<Entry<Integer, Integer>> collect = counts.entrySet().stream()
					.sorted((e1, e2) -> -Integer.compare(e1.getValue(), e2.getValue()))
					.collect(Collectors.toList());
			List<Integer> top = collect.stream().limit(limit).map(e -> e.getKey()).collect(Collectors.toList());
			return top;
		}
	}
}
