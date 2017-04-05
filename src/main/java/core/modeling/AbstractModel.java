package core.modeling;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import core.modeling.mix.MixModel;
import core.modeling.ngram.NGramModel;
import core.util.Pair;

/**
 * Implementation of {@link core.mode.Model} interface that serves as base class for most models.<br />
 * This class extends {@link Model} by imposing per-token control and maintenance
 * 
 * @see {@link MixModel}, {@link NGramModel}
 * 
 * @author Vincent Hellendoorn
 *
 */
public abstract class AbstractModel implements Model {
	
	protected boolean dynamic = false;
	
	/**
	 * Enable/disable dynamic updating of model, as implemented by underlying model.
	 * @param dynamic True if the model should update dynamically
	 */
	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	/**
	 * Default implementation of {@link #model(List)},
	 * which invokes {@link #modelToken(List, int)} at each index and takes care of dynamic updating after each token.
	 * Can be overriden in favor of batch processing by underlying class if preferable.
	 */
	public final List<Pair<Double, Double>> model(List<Integer> input) {
		return IntStream.range(0, input.size()).mapToObj(i -> {
			Pair<Double, Double> modeled = modelToken(input, i);
			if (this.dynamic) this.learnToken(input, i);
			return modeled;
		}).collect(Collectors.toList());
	}
	
	/**
	 * Default implementation of {@link #model(List)},
	 * which invokes {@link #modelToken(List, int)} at each index and takes care of dynamic updating after each token.
	 * Can be overriden in favor of batch processing by underlying class if preferable.
	 */
	public final List<Map<Integer, Pair<Double, Double>>> predict(List<Integer> input) {
		boolean temp = this.dynamic;
		this.setDynamic(false);
		List<Map<Integer, Pair<Double, Double>>> res = IntStream.range(0, input.size()).mapToObj(i -> {
			Map<Integer, Pair<Double, Double>> predictions = predictToken(input, i);
			if (temp) this.learnToken(input, i);
			return predictions;
		}).collect(Collectors.toList());
		this.setDynamic(temp);
		return res;
	}
}
