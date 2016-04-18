package slp.core.modeling;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.util.Pair;

public interface Model {

	public default Stream<Double> modelStream(Stream<Stream<Integer>> in) {
		return in.map(this::model);
	}
	
	public default double model(Stream<Integer> in) {
		// Very rare (one in a million) "sanity check"
		if (Math.random() < 10E-6) {
			in = optionalSanityCheck(in);
		}
		return this.modelSequence(in);
	}

	public default Stream<Integer> optionalSanityCheck(Stream<Integer> in) {
		List<Integer> collect = in.collect(Collectors.toList());
		int old = collect.get(collect.size() - 1);
		double sum = 0.0;
		for (int i = 0; i < Vocabulary.size; i++) {
			collect.set(collect.size() - 1, i);
			double prob = this.modelSequence(collect.stream());
			sum += prob;
		}
		if (Math.abs(sum - 1.0) > 0.01) {
			System.err.println("Sanity check failed! For sequence " + collect + " (total probability sums to: "+ sum + ")");
		}
		collect.set(collect.size() - 1, old);
		return collect.stream();
	}
	
	double modelSequence(Stream<Integer> in);
	
	/**
	 * Returns a pair containing both the probability assigned to this sequence and the
	 * self-reported confidence in this probability
	 * @param in
	 * @return
	 */
	// TODO: make mandatory
	public default Pair<Double, Double> modelWithConfidence(Stream<Integer> in) {
		return Pair.of(modelSequence(in), 1.0);
	}
	
	public static Model standard(Counter counter) {
		return new AbsDiscModel(counter);
	}
}
