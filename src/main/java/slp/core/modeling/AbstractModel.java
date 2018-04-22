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
	private boolean wasDynamic = false;
	private int dynamicDepth = 0;
	
	/**
	 * Enable/disable dynamic updating of model, as implemented by underlying model.
	 * @param dynamic True if the model should update dynamically
	 */
	@Override
	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
		this.wasDynamic = dynamic;
	}
	
	@Override
	public void pauseDynamic() {
		this.dynamicDepth++;
		this.dynamic = false;
	}

	@Override
	public void unPauseDynamic() {
		if (this.wasDynamic && this.dynamicDepth > 0 && --this.dynamicDepth == 0) {
			this.dynamic = true;
		}
	}
	
	@Override
	public double getConfidence(List<Integer> input, int index) {
		this.pauseDynamic();
		double confidence = this.predictAtIndex(input, index).entrySet().stream()
			.map(e -> e.getValue().left)
			.sorted((p1, p2) -> -Double.compare(p1, p2))
			.mapToDouble(d -> d).limit(1).sum();
		this.unPauseDynamic();
		return confidence;
	}

	/**
	 * Default implementation of {@link #modelToken(List, int)},
	 * which invokes {@link #modelAtIndex(List, int)} at each index and takes care of dynamic updating after each token.
	 */
	@Override
	public final Pair<Double, Double> modelToken(List<Integer> input, int index) {
		Pair<Double, Double> modeled = modelAtIndex(input, index);
		if (this.dynamic) this.learnToken(input, index);
		return modeled;
	}
	
	public abstract Pair<Double, Double> modelAtIndex(List<Integer> input, int index);
	
	/**
	 * Default implementation of {@link #predictToken(List, int)},
	 * which invokes {@link #predictAtIndex(List, int)} at each index and takes care of dynamic updating for each token.
	 */
	@Override
	public final Map<Integer, Pair<Double, Double>> predictToken(List<Integer> input, int index) {
		boolean temp = this.dynamic;
		this.setDynamic(false);
		Map<Integer, Pair<Double, Double>> predictions = predictAtIndex(input, index);
		this.setDynamic(temp);
		if (this.dynamic) this.learnToken(input, index);
		return predictions;
	}
	
	public abstract Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index);

}
