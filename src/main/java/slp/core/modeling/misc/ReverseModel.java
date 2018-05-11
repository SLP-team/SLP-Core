package slp.core.modeling.misc;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.modeling.AbstractModel;
import slp.core.modeling.Model;
import slp.core.util.Pair;

public class ReverseModel extends AbstractModel {

	private final Model model;

	public ReverseModel(Model model) {
		this.model = model;
	}

	private List<Integer> getReverse(List<Integer> input) {
		return IntStream.range(0, input.size())
				.mapToObj(i -> input.get(input.size() - i - 1))
				.collect(Collectors.toList());
	}
	
	@Override
	public void notify(File next) {
		this.model.notify(next);
	}

	@Override
	public void learn(List<Integer> input) {
		input = getReverse(input);
		this.model.learn(input);
	}

	@Override
	public void learnToken(List<Integer> input, int index) {
		input = getReverse(input);
		this.model.learnToken(input, input.size() - index - 1);
	}
	
	@Override
	public void forget(List<Integer> input) {
		input = getReverse(input);
		this.model.forget(input);
	}

	@Override
	public void forgetToken(List<Integer> input, int index) {
		input = getReverse(input);
		this.model.forgetToken(input, input.size() - index - 1);
	}

	@Override
	public List<Pair<Double, Double>> model(List<Integer> input) {
		input = getReverse(input);
		List<Pair<Double, Double>> modeled = this.model.model(input);
		Collections.reverse(modeled);
		return modeled;
	}
	
	@Override
	public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
		input = getReverse(input);
		Pair<Double, Double> modeled = this.model.modelToken(input, input.size() - index - 1);
		return modeled;
	}
	
	@Override
	public List<Map<Integer, Pair<Double, Double>>> predict(List<Integer> input) {
		input = getReverse(input);
		List<Map<Integer, Pair<Double, Double>>> predictions = this.model.predict(input);
		Collections.reverse(predictions);
		return predictions;
	}
	
	@Override
	public Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index) {
		input = getReverse(input);
		Map<Integer, Pair<Double, Double>> prediction = this.model.predictToken(input, input.size() - index - 1);
		return prediction;
	}
	
}
