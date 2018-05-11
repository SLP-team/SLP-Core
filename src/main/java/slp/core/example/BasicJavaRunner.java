package slp.core.example;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Stream;

import slp.core.counting.giga.GigaCounter;
import slp.core.lexing.Lexer;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.dynamic.CacheModel;
import slp.core.modeling.dynamic.NestedModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;

/**
 * This example shows a typical use-case for (Java) source code of this tool with detailed comments.
 * We setup a {@link LexerRunner} with a {@link Lexer}, train a {@link Model} using a {@link ModelRunner}
 * and print the overall result. This is a good starting point for understanding the tool's API.
 * See also the {@link BasicNLRunner} for an equivalent example with typical settings for natural language modeling tasks.
 * <br /><br />
 * More complex use-cases can be found in the other examples, such finding entropy for each token and line,
 * using a bi-directional model (in parallel with others), .
 * 
 * @author Vincent Hellendoorn
 */
public class BasicJavaRunner {
	public static void main(String[] args) {
		if (args.length < 1) return;
		// Assumes at least one argument, the path (file or directory) to train on
		File train = new File(args[0]);
		// If second argument, will test on that path, else will 'self-test' on train using full cross-validation per line
		File test = args.length < 2 ? train : new File(args[1]);

		// 1. Lexing
		//   a. Set up lexer using a JavaLexer
		//		- The second parameter informs it that we want to files as a block, not line by line
		Lexer lexer = new JavaLexer();
		LexerRunner lexerRunner = new LexerRunner(lexer, false);
		//   b. Since our data does not contain sentence markers (for the start and end of each file), add these here
		//		- The model will assume that these markers are present and always skip the first token when modeling
		lexerRunner.setSentenceMarkers(true);
		//   c. Only lex (and model) files that end with "java". See also 'setRegex'
		lexerRunner.setExtension("java");
		
		
		// 2. Vocabulary:
		//    - For code, we typically make an empty vocabulary and let it be built while training.
		//    - Building it first using the default settings (no cut-off, don't close after building)
		//		should yield the same result, and may be useful if you want to write the vocabulary before training.
		//    - If interested, use: VocabularyRunner.build(lexerRunner, train);
		Vocabulary vocabulary = new Vocabulary();
		
		
		// 3. Model
		//	  a. We will use an n-gram model with simple Jelinek-Mercer smoothing (works well for code)
		//		 - The n-gram order of 6 is used, which is also the standard
		//       - Let's use a GigaCounter (useful for large corpora) here as well; the nested model later on will copy this behavior.
		Model model = new JMModel(6, new GigaCounter());
		//    b. We can get more fancy by converting the model into a complex mix model.
		//       - For instance, we can wrap the ModelRunner in a nested model, causing it to learn all files in test into a new 'local' model
		//		 - Most mixed models don't need access to a LexerRunner or vocabulary, but NestedModel actually creates new Models on the fly
		model = new NestedModel(model, lexerRunner, vocabulary, test);
		//       - Then, add an ngram-cache component.
		//         * Order matters here; the last model in the mix gets the final say and thus the most importance
		model = MixModel.standard(model, new CacheModel());
		//       - Finally, we can enable dynamic updating for the whole mixture (won't affect cache; it's dynamic by default)
		model.setDynamic(true);
		//	  c. We create a ModelRunner with this model and ask it to learn the train directory
		//		 - This invokes Model.learn for each file, which is fine for n-gram models since these are count-based;
		//         other model implementations may prefer to train in their own way.
		ModelRunner modelRunner = new ModelRunner(model, lexerRunner, vocabulary);
		modelRunner.learnDirectory(train);
		//    d. We assume you are self-testing if the train and test directory are equal.
		//		 This will make it temporarily forget any sequence to model, effectively letting you train and test on all your data
		modelRunner.setSelfTesting(train.equals(test));
		//		 - If you plan on using a NestedModel to self-test, you can also just remove the two above calls;
		//		   they teach the global model the same things that the nested model knows, which barely boosts performance.
		
		
		// 4. Running
		//    a. Finally, we model each file in 'test' recursively
		Stream<Pair<File, List<List<Double>>>> modeledFiles = modelRunner.modelDirectory(test);
		//	  b. Retrieve overall entropy statistics using ModelRunner's convenience method
		DoubleSummaryStatistics statistics = modelRunner.getStats(modeledFiles);
		System.out.printf("Modeled %d tokens, average entropy:\t%.4f\n", statistics.getCount(), statistics.getAverage());

		/*
		 * Note: the above consumes the per-token entropies, so they are lost afterwards.
		 * If you'd like to store them first, use something like:
		 * 
		 *  Map<File, List<List<Double>>> stored =
		 * 		modeledFiles.collect(Collectors.toMap(Pair::left, Pair::right));
		 * 
		 * This may consume a sizeable chunk of RAM across millions of tokens
		 */
	}
}
