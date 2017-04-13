package slp.core.modeling;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.modeling.mix.MixModel;
import slp.core.modeling.mix.NestedModel;
import slp.core.modeling.ngram.NGramModel;
import slp.core.util.Pair;

/**
 * Interface for models, providing the third step after lexing and translating.
 * Implemented primarily through {@link AbstractModel} and {@link MixModel}.
 * <br /><br />
 * The interface allows a model to be notified when modeling a new file and furthermore specifies
 * two types of updating (learning and forgetting) and two types of modeling (entropy and prediction).
 * Each update and model operation comes in two flavors: batch and indexed:
 * <ul>
 * <li> The <b>indexed</b> mode is essential for a model to support, allowing for such tasks 
 * as on-the-fly prediction, updating cache models after seeing each token, etc.
 * <li> The <b>batch</b> mode is added because it can yield quite substantial speed-up for some models and 
 * should thus be invoked as often as possible (e.g. as in {@link ModelRunner}).
 * It is implemented with simple iteration over calls to indexed mode by default but overriding this is encouraged.
 * </ul>
 * See also {@link AbstractModel}, which overrides {@link #model(List)} and {@link #predict(List)} to incorporate dynamic
 * updating to models.
 * come with a default implementation which simply invokes the indexed version for each index.
 * 
 * @author Vincent Hellendoorn
 *
 */
public interface Model {
	
	/**
	 * Notifies model of upcoming test file, allowing it to set up accordingly (e.g. for nested models)
	 * <br />
	 * <em>Note:</em> most models may simply do nothing, but is tentatively left <code>abstract</code> as a reminder.
	 * 
	 * @param next File to model next
	 */
	void notify(File next);

	// TODO
	void setDynamic(boolean dynamic);
	
	/**
	 * Notify underlying model to learn provided input. May be ignored if no such ability exists.
	 * Default implementation simply invokes {@link #learnToken(List, int)} for each position in input.
	 * 
	 * @see {@link #learnToken(List, int)}, {@link #forget(List)}
	 * 
	 * @param input Lexed and translated input tokens (use {@code Vocabulary} to translate back if needed)
	 */
	default void learn(List<Integer> input) {
		IntStream.range(0, input.size()).forEach(i -> this.learnToken(input, i));
	}
	
	/**
	 * Like {@link #learn(List)} but for the specific token at {@code index}.
	 * Primarily used for dynamic updating. Similar to {@link #modelToken(List, int)},
	 * batch implementation ({@link #learn(List)} should be invoked when possible and can provide speed-up.
	 * 
	 * @param input Lexed and translated input tokens (use {@code Vocabulary} to translate back if needed)
	 * @param index Index of token to assign probability/confidence score too.
	 */
	void learnToken(List<Integer> input, int index);
	
	/**
	 * Notify underlying model to 'forget' the provided input, e.g. prior to self-testing.
	 * May be ignored if no such ability exists.
	 * Default implementation simply invokes {@link #forgetToken(List, int)} for each position in input.
	 * 
	 * <br /><br />
	 * Any invoking code should note the risk of the underlying model not implementing this!
	 * For instance when self-testing, this may lead to testing on train-data.
	 * <br />
	 * See {@link NestedModel} for a good example, which uses this functionality to train on all-but the test file.
	 * It currently uses only {@link NGramModel}s which are capable of un-learning input.
	 * 
	 * @see {@link #forgetToken(List, int)}, {@link #learn(List)}
	 * 
	 * @param input Lexed and translated input tokens (use {@code Vocabulary} to translate back if needed)
	 */
	default void forget(List<Integer> input) {
		IntStream.range(0, input.size()).forEach(i -> this.forgetToken(input, i));
	}
	
	/**
	 * Like {@link #forget(List)} but for the specific token at {@code index}.
	 * Primarily used for dynamic updating. Similar to {@link #modelToken(List, int)},
	 * batch implementation ({@link #forget(List)} should be invoked when possible and can provide speed-up.
	 * 
	 * @param input Lexed and translated input tokens (use {@code Vocabulary} to translate back if needed)
	 * @param index Index of token to assign probability/confidence score too.
	 */
	void forgetToken(List<Integer> input, int index);


	/**
	 * Model each token in input to a pair of probability/confidence (see {@link #modelToken(List, int)}.
	 * <br />
	 * The default implementation simply invokes {@link #modelToken(List, int)} for each index;
	 * can be overriden in favor of batch processing by underlying class if preferable
	 * (but remember to implement dynamic updating or caches won't work).
	 * 
	 * @param input Lexed and translated input tokens (use {@code Vocabulary} to translate back if needed)
	 * 
	 * @return Probability/Confidence Pair for each token in input
	 */
	default List<Pair<Double, Double>> model(List<Integer> input) {
		return IntStream.range(0, input.size())
				.mapToObj(i -> modelToken(input, i))
				.collect(Collectors.toList());
	}
	
	/**
	 * Model a single token in {@code input} at index {@code index} to a pair of (probability, confidence) &isin; ([0,1], [0,1])
	 * The probability must be a valid probability, positive and summing to 1 given the context.
	 * <br />
	 * {@link AbstractModel} implements this with dynamic updating support.
	 * <br />
	 * Since some models implement faster "batch" processing, {@link #model(List)} 
	 * should generally be called if possible.
	 * 
	 * @param input Lexed and translated input tokens (use {@code Vocabulary} to translate back if needed)
	 * @param index Index of token to assign probability/confidence score too.
	 * 
	 * @return Probability/Confidence Pair for token at {@code index} in {@code input}
	 */
	Pair<Double, Double> modelToken(List<Integer> input, int index);
	
	/**
	 * Give top <code>N</code> predictions for each token in input with probability/confidence scores (see {@link #modelToken(List, int)}.
	 * <br />
	 * The default implementation simply invokes {@link #predictToken(List, int)} for each index;
	 * can be overriden in favor of batch processing by underlying class if preferable
	 * (but remember to implement dynamic updating or caches won't work).
	 * 
	 * @param input Lexed and translated input tokens (use {@code Vocabulary} to translate back if needed)
	 * @return Probability/Confidence-weighted set of predictions for each token in input
	 */
	default List<Map<Integer, Pair<Double, Double>>> predict(List<Integer> input) {
		return IntStream.range(0, input.size())
				.mapToObj(i -> predictToken(input, i))
				.collect(Collectors.toList());
	}
	
	/**
	 * Give top <code>N</code> predictions for position {@code index} in input,
	 * with corresponding probability/confidence scores as in {@link #modelToken(List, int)}.
	 * <br />
	 * {@link AbstractModel} implements this with dynamic updating support.
	 * <br />
	 * The model should produce suggestions for the token that should appear
	 * following the first {@code index} tokens in {@code input}, regardless of what token is presently there
	 * if any (e.g. it could be an insertion task).
	 * Some example interpretations for different values of {@code index}:
	 * <ul>
	 * <li> 0: predict the first token, without context.
	 * <li> {@code input.size()}: predict the next token after {@code input}.
	 * <li> 3: predict the 4th token in {@code input}, regardless of what token is currently at that index.
	 * </ul>
	 * <br />
	 * Since some models implement faster "batch" processing, {@link #predict(List)} 
	 * should generally be called if possible.
	 * 
	 * @param input Lexed and translated input tokens (use {@code Vocabulary} to translate back if needed)
	 * @param index Index of token to assign probability/confidence score too.
	 * 
	 * @return Probability/Confidence Pair for token at {@code index} in {@code input}
	 */
	Map<Integer, Pair<Double, Double>> predictToken(List<Integer> input, int index);
}
