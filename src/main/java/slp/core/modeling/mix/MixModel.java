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
	
	public static MixModel standard(Model model1, Model model2) {
		return new InverseMixModel(model1, model2);
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
		notifyLeft(next);
		notifyRight(next);
	}

	protected void notifyLeft(File next) {
		this.left.notify(next);
	}

	protected void notifyRight(File next) {
		this.right.notify(next);
	}
	
	@Override
	public void setDynamic(boolean dynamic) {
		this.left.setDynamic(dynamic);
		this.right.setDynamic(dynamic);
	}

	@Override
	public void pauseDynamic() {
		this.left.pauseDynamic();
		this.right.pauseDynamic();
	}

	@Override
	public void unPauseDynamic() {
		this.left.unPauseDynamic();
		this.right.unPauseDynamic();
	}

	@Override
	public void learn(List<Integer> input) {
		learnLeft(input);
		learnRight(input);
	}

	protected void learnLeft(List<Integer> input) {
		this.left.learn(input);
	}

	protected void learnRight(List<Integer> input) {
		this.right.learn(input);
	}

	@Override
	public void learnToken(List<Integer> input, int index) {
		learnLeft(input, index);
		learnRight(input, index);
	}

	protected void learnLeft(List<Integer> input, int index) {
		this.left.learnToken(input, index);
	}

	protected void learnRight(List<Integer> input, int index) {
		this.right.learnToken(input, index);
	}
	
	@Override
	public void forget(List<Integer> input) {
		forgetLeft(input);
		forgetRight(input);
	}

	protected void forgetLeft(List<Integer> input) {
		this.left.forget(input);
	}

	protected void forgetRight(List<Integer> input) {
		this.right.forget(input);
	}

	@Override
	public void forgetToken(List<Integer> input, int index) {
		forgetLeft(input, index);
		forgetRight(input, index);
	}

	protected void forgetLeft(List<Integer> input, int index) {
		this.left.forgetToken(input, index);
	}

	protected void forgetRight(List<Integer> input, int index) {
		this.right.forgetToken(input, index);
	}
	
	@Override
	public List<Pair<Double, Double>> model(List<Integer> input) {
		List<Pair<Double, Double>> modelL = modelLeft(input);
		List<Pair<Double, Double>> modelR = modelRight(input);
		return IntStream.range(0, input.size())
				.mapToObj(i -> mix(input, i, modelL.get(i), modelR.get(i)))
				.collect(Collectors.toList());
	}

	protected List<Pair<Double, Double>> modelLeft(List<Integer> input) {
		return this.left.model(input);
	}

	protected List<Pair<Double, Double>> modelRight(List<Integer> input) {
		return this.right.model(input);
	}

	@Override
	public Pair<Double, Double> modelToken(List<Integer> input, int index) {
		Pair<Double, Double> res1 = modelLeft(input, index);
		Pair<Double, Double> res2 = modelRight(input, index);
		return mix(input, index, res1, res2);
	}

	protected Pair<Double, Double> modelLeft(List<Integer> input, int index) {
		return this.left.modelToken(input, index);
	}
	
	protected Pair<Double, Double> modelRight(List<Integer> input, int index) {
		return this.right.modelToken(input, index);
	}

	@Override
	public List<Map<Integer, Pair<Double, Double>>> predict(List<Integer> input) {
		List<Map<Integer, Pair<Double, Double>>> predictL = predictLeft(input);
		List<Map<Integer, Pair<Double, Double>>> predictR = predictRight(input);
		return IntStream.range(0, input.size())
				.mapToObj(i -> mix(input, i, predictL.get(i), predictR.get(i)))
				.collect(Collectors.toList());
	}

	protected List<Map<Integer, Pair<Double, Double>>> predictLeft(List<Integer> input) {
		return this.left.predict(input);
	}

	protected List<Map<Integer, Pair<Double, Double>>> predictRight(List<Integer> input) {
		return this.right.predict(input);
	}

	@Override
	public Map<Integer, Pair<Double, Double>> predictToken(List<Integer> input, int index) {
		Map<Integer, Pair<Double, Double>> res1 = predictLeft(input, index);
		Map<Integer, Pair<Double, Double>> res2 = predictRight(input, index);
		return mix(input, index, res1, res2);
	}

	protected Map<Integer, Pair<Double, Double>> predictLeft(List<Integer> input, int index) {
		return this.left.predictToken(input, index);
	}

	protected Map<Integer, Pair<Double, Double>> predictRight(List<Integer> input, int index) {
		return this.right.predictToken(input, index);
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
		this.left.pauseDynamic();
		this.right.pauseDynamic();
		for (int key : res1.keySet()) {
			Pair<Double, Double> own = res1.get(key);
			Pair<Double, Double> other = res2.get(key);
			if (other == null) {
				boolean added = index == input.size();
				if (added) input.add(0);
				int prev = input.set(index, key);
				other = modelRight(input, index);
				if (added) input.remove(input.size() - 1);
				else input.set(index, prev);
			}
			mixed.put(key, mix(input, index, own, other));
		}
		for (int key : res2.keySet()) {
			if (res1.containsKey(key)) continue;
			Pair<Double, Double> own = res2.get(key);
			boolean added = index == input.size();
			if (added) input.add(0);
			int prev = input.set(index, key);
			Pair<Double, Double> other = modelLeft(input, index);
			if (added) input.remove(input.size() - 1);
			else input.set(index, prev);
			mixed.put(key, mix(input, index, own, other));
		}
		this.left.unPauseDynamic();
		this.right.unPauseDynamic();
		return mixed;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + this.left.toString() + ", " + this.right.toString() + "]";
	}
}
