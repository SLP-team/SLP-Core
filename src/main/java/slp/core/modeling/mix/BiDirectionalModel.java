package slp.core.modeling.mix;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.lexing.Lexer;
import slp.core.lexing.LexerRunner;
import slp.core.lexing.util.ReverseLexer;
import slp.core.modeling.Model;
import slp.core.util.Pair;

/**
 * @author Vincent Hellendoorn
 *
 */
public class BiDirectionalModel extends MixModel {

	/**
	 * Use only for stateless models!
	 */
	public BiDirectionalModel(Model model) {
		super(model, model);
	}

	public BiDirectionalModel(Model model1, Model model2) {
		super(model1, model2);
	}

	public Model getForward() {
		return this.left;
	}
	
	public Model getReverse() {
		return this.right;
	}

	private List<Integer> getReverse(List<Integer> input) {
		// A reverse lexer is set and un-set by notify and may thus already be available
		// If it is not set, explicitly reverse the input
		if (!(LexerRunner.getLexer() instanceof ReverseLexer)) {
			return IntStream.range(0, input.size())
					.mapToObj(i -> input.get(input.size() - i - 1))
					.collect(Collectors.toList());
		}
		return input;
	}

	@Override
	protected void notifyRight(File next) {
		Lexer lexer = LexerRunner.getLexer();
		LexerRunner.setLexer(new ReverseLexer(lexer));
		this.right.notify(next);
		LexerRunner.setLexer(lexer);
	}
	
	@Override
	protected void learnRight(List<Integer> input) {
		input = getReverse(input);
		this.right.learn(input);
	}

	@Override
	protected void learnRight(List<Integer> input, int index) {
		input = getReverse(input);
		this.right.learnToken(input, input.size() - index - 1);
	}
	
	@Override
	protected void forgetRight(List<Integer> input) {
		input = getReverse(input);
		this.right.forget(input);
	}

	@Override
	protected void forgetRight(List<Integer> input, int index) {
		input = getReverse(input);
		this.right.forgetToken(input, input.size() - index - 1);
	}

	@Override
	protected List<Pair<Double, Double>> modelRight(List<Integer> input) {
		input = getReverse(input);
		List<Pair<Double, Double>> modeled = this.right.model(input);
		Collections.reverse(modeled);
		return modeled;
	}
	
	@Override
	protected Pair<Double, Double> modelRight(List<Integer> input, int index) {
		input = getReverse(input);
		Pair<Double, Double> modeled = this.right.modelToken(input, input.size() - index - 1);
		return modeled;
	}
	
	@Override
	protected List<Map<Integer, Pair<Double, Double>>> predictRight(List<Integer> input) {
		input = getReverse(input);
		List<Map<Integer, Pair<Double, Double>>> predictions = this.right.predict(input);
		Collections.reverse(predictions);
		return predictions;
	}
	
	@Override
	protected Map<Integer, Pair<Double, Double>> predictRight(List<Integer> input, int index) {
		input = getReverse(input);
		Map<Integer, Pair<Double, Double>> prediction = this.right.predictToken(input, input.size() - index - 1);
		return prediction;
	}

	@Override
	protected Pair<Double, Double> mix(List<Integer> input, int index, Pair<Double, Double> res1, Pair<Double, Double> res2) {
		if (res1.right.equals(0.0) && res2.right.equals(0.0)) {
			return Pair.of(0.0, 0.0);
		}
		else if (res2.right.equals(0.0)) return res1;
		else if (res1.right.equals(0.0)) return res2;
		double lNorm = 1/(1 - res1.right);
		double rNorm = 1/(1 - res2.right);
		if (lNorm > 1000) lNorm = 1000;
		if (rNorm > 1000) rNorm = 1000;
		double probability = (res1.left*lNorm + res2.left*rNorm)/(lNorm + rNorm);
		double confidence = Math.max(res1.right, res2.right);
		return Pair.of(probability, confidence);
	}
}
