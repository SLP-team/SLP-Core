package slp.core.modeling.mix;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.modeling.Model;
import slp.core.util.Pair;

public abstract class MixModel implements Model {
	protected Model left;
	protected Model right;
	
	public MixModel(Model model1, Model model2) {
		this.left = model1;
		this.right = model2;
	}
	
	public Model getLeft() {
		return this.left;
	}
	
	public Model getRight() {
		return this.right;
	}

	public void setLeft(Model model) {
		this.left = model;
	}
	
	public void setRight(Model model) {
		this.right = model;
	}
	
	@Override
	public void notify(File next) {
		this.left.notify(next);
		this.right.notify(next);
	}
	
	@Override
	public void setDynamic(boolean dynamic) {
		this.left.setDynamic(dynamic);
		this.right.setDynamic(dynamic);
	}

	@Override
	public void learn(List<Integer> input) {
		this.left.learn(input);
		this.right.learn(input);
	}

	@Override
	public void learnToken(List<Integer> input, int index) {
		this.left.learnToken(input, index);
		this.right.learnToken(input, index);
	}
	
	@Override
	public void forget(List<Integer> input) {
		this.left.forget(input);
		this.right.forget(input);
	}

	@Override
	public void forgetToken(List<Integer> input, int index) {
		this.left.forgetToken(input, index);
		this.right.forgetToken(input, index);
	}
	
	@Override
	public List<Pair<Double, Double>> model(List<Integer> input) {
		List<Pair<Double, Double>> modelL = this.left.model(input);
		List<Pair<Double, Double>> modelR = this.right.model(input);
		return IntStream.range(0, input.size())
				.mapToObj(i -> mix(input, i, modelL.get(i), modelR.get(i)))
				.collect(Collectors.toList());
	}

	@Override
	public Pair<Double, Double> modelToken(List<Integer> input, int index) {
		Pair<Double, Double> res1 = this.left.modelToken(input, index);
		Pair<Double, Double> res2 = this.right.modelToken(input, index);
		return mix(input, index, res1, res2);
	}
	
	@Override
	public List<Map<Integer, Pair<Double, Double>>> predict(List<Integer> input) {
		List<Map<Integer, Pair<Double, Double>>> predictL = this.left.predict(input);
		List<Map<Integer, Pair<Double, Double>>> predictR = this.right.predict(input);
		return IntStream.range(0, input.size())
				.mapToObj(i -> mix(input, i, predictL.get(i), predictR.get(i)))
				.collect(Collectors.toList());
	}

	@Override
	public Map<Integer, Pair<Double, Double>> predictToken(List<Integer> input, int index) {
		Map<Integer, Pair<Double, Double>> res1 = this.left.predictToken(input, index);
		Map<Integer, Pair<Double, Double>> res2 = this.right.predictToken(input, index);
		return mix(input, index, res1, res2);
	}

	/**
	 * Mix a pair of probability/confidence for both the left and right model according to some function.
	 * @param input The lexed and translated input
	 * @param index The index of the token to model in the input
	 * @param res1 Left model's score
	 * @param res2 Right model's score
	 * @return A mixture of the two scores
	 */
	protected abstract Pair<Double, Double> mix(List<Integer> input, int index, Pair<Double, Double> res1, Pair<Double, Double> res2);
	
	/**
	 * Default mixing implementation of predictions at {@code index} from both models.
	 * Acquires a consolidated map of predictions, invoking the other model for any non-overlapping predictions.
	 * <br />
	 * An alternative, faster implementation could be to simply assume zero confidence
	 * for any non-overlapping keys, which would be more compatible with batch mode.
	 */
	protected Map<Integer, Pair<Double, Double>> mix(List<Integer> input, int index,
			Map<Integer, Pair<Double, Double>> res1, Map<Integer, Pair<Double, Double>> res2) {
		Map<Integer, Pair<Double, Double>> mixed = new HashMap<>();
		for (int key : res1.keySet()) {
			Pair<Double, Double> own = res1.get(key);
			Pair<Double, Double> other = res2.get(key);
			if (other == null) {
				Integer prev = input.set(index, key);
				other = this.right.modelToken(input, index);
				input.set(index, prev);
			}
			mixed.put(key, mix(input, index, own, other));
		}
		for (int key : res2.keySet()) {
			if (res1.containsKey(key)) continue;
			Pair<Double, Double> own = res2.get(key);
			Integer prev = input.set(index, key);
			Pair<Double, Double> other = this.left.modelToken(input, index);
			input.set(index, prev);
			mixed.put(key, mix(input, index, own, other));
		}
		return mixed;
	}
}
