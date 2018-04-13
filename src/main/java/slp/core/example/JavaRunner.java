package slp.core.example;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Stream;

import slp.core.counting.giga.GigaCounter;
import slp.core.lexing.Lexer;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.CacheModel;
import slp.core.modeling.Model;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.util.Pair;

public class JavaRunner {
	public static void main(String[] args) {
		if (args.length < 1) return;
		// Assumes at least one argument, the path (file or directory) to train on
		File train = new File(args[0]);
		// If second argument, will test on that path, else will 'self-test' on train using full cross-validation per line
		File test = args.length < 2 ? train : new File(args[1]);

		// 1. Lexing
		//   a. Set up lexer using a JavaLexer
		Lexer lexer = new JavaLexer();
		LexerRunner lexerRunner = new LexerRunner(lexer);
		//   b. Do not tokenize per line for Java (invoked for the sake of example; false is the default)
		lexerRunner.setPerLine(false);
		//   c. But, do add sentence markers (for the start and end of each file)
		lexerRunner.setSentenceMarkers(true);
		//   d. Only lex (and thus implicitly, model) files that end with "java". See also 'setRegex'
		lexerRunner.setExtension("java");
		
		
		// 2. Vocabulary:
		//    - Since we are not modifying the defaults in any way, we'll just let the vocabulary be built while training.
		//    - Building it first using the default settings (no cut-off, don't close after building)
		//		should yield the same result, and may be useful if you want to write the vocabulary before training.
		//    - If interested, use: VocabularyRunner.build(lexer, train);
		
		
		// 3. Model
		//	  a. We will use an n-gram model with simple Jelinek-Mercer smoothing (works well for code)
		//		 - The n-gram order of 6 is used, which is also the standard
		//       - Let's use a GigaCounter (useful for large corpora) here as well; the nested model later on will copy this behavior.
		Model model = new JMModel(6, new GigaCounter());
		//       - We pass this Model to a ModelRunner, which takes care of most of the work and configuration for us
		ModelRunner modelRunner = new ModelRunner(lexerRunner, model);
		//    b. Self-testing if train is equal to test; this will un-count each file before modeling it.
		modelRunner.setSelfTesting(train.equals(test));
		//    c. First, train this model on all files in 'train' recursively, using the usual updating mechanism (same as for dynamic updating).
		//       - Note that this invokes Model.learn for each file, which is fine for n-gram models since these are count-based;
		//          other models may prefer to pre-train when calling the Model's constructor.
		modelRunner.learn(train);
		//    d. To get more fancy for source code, we can convert the model into a complex mix model.
		//       - First wrap in a nested model, causing it to learn all files in test into a new 'local' model
		modelRunner.setNested(test);
		//       - Then, add an ngram-cache component.
		//         * Note, order matters! The most local model should be "right-most" so it is called upon last (i.e. "gets the final say")
		//         * This is also why we apply the cache after the nested model
		model = MixModel.standard(model, new CacheModel());
		//       - Finally, enable dynamic updating for the whole mixture (won't affect cache; it's dynamic by default)
		model.setDynamic(true);
		
		
		// 4. Running
		//    a. Model each file in 'test' recursively
		Stream<Pair<File, List<List<Double>>>> modeledFiles = modelRunner.model(test);
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
