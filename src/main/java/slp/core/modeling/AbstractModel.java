package slp.core.modeling;

import java.util.List;
import java.util.Map;

import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.NGramModel;
import slp.core.util.Pair;

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
	 * Default implementation of {@link #modelToken(List, int)},
	 * which invokes {@link #modelAtIndex(List, int)} at each index and takes care of dynamic updating after each token.
	 */
	public final Pair<Double, Double> modelToken(List<Integer> input, int i) {
		Pair<Double, Double> modeled = modelAtIndex(input, i);
		if (this.dynamic) this.learnToken(input, i);
		return modeled;
	}
	
	public abstract Pair<Double, Double> modelAtIndex(List<Integer> input, int i);
	
	/**
	 * Default implementation of {@link #predictToken(List, int)},
	 * which invokes {@link #predictAtIndex(List, int)} at each index and takes care of dynamic updating for each token.
	 */
	public final Map<Integer, Pair<Double, Double>> predictToken(List<Integer> input, int i) {
		boolean temp = this.dynamic;
		this.setDynamic(false);
		Map<Integer, Pair<Double, Double>> predictions = predictAtIndex(input, i);
		this.setDynamic(temp);
		if (this.dynamic) this.learnToken(input, i);
		return predictions;
	}
	
	public abstract Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int i);

}
