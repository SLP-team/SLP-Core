package slp.core.example;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Stream;

import slp.core.counting.giga.GigaCounter;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.CacheModel;
import slp.core.modeling.Model;
import slp.core.modeling.mix.InverseMixModel;
import slp.core.modeling.mix.NestedModel;
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
		LexerRunner lexerRunner = new LexerRunner(new JavaLexer());
		//   b. Do not tokenize per line for Java (invoked for the sake of example; false is the default)
		lexerRunner.setPerLine(false);
		//   c. But, do add sentence markers (to start and end of file)
		lexerRunner.setSentenceMarkers(true);
		//   d. Only lex (and thus implicitly, model) files ending with "java". See also 'useRegex'
		lexerRunner.setExtension("java");
		
		// 2. Vocabulary:
		//    - Since we are not modifying the defaults in any way, we'll just let the vocabulary be built while training.
		//    - Building it first using the default settings (no cut-off, don't close after building)
		//		should yield the same result, but could e.g. allow one to write the vocabulary before training.
		//    - If interested, use: VocabularyRunner.build(train);
		
		// 3. Model
		//    a. No per line modeling for Java (default)
		ModelRunner modelRunner = new ModelRunner(lexerRunner);
		modelRunner.perLine(false);
		//    b. Self-testing if train is equal to test; will un-count each file before modeling it.
		modelRunner.selfTesting(train.equals(test));
		//    c. Set n-gram model order, 6 works well for Java (and is the default)
		//    d. Use an n-gram model with simple Jelinek-Mercer smoothing (works well for code)
		//       - Let's use a GigaCounter, fit for large corpora here as well; the nested model later on will copy this behavior.
		Model model = new JMModel(6, new GigaCounter());
		//    e. First, train this model on all files in 'train' recursively, using the usual updating mechanism (same as for dynamic updating).
		//       - Note that this invokes Model.learn for each file, which is fine for n-gram models since these are count-based;
		//          other models may prefer to pre-train when calling the Model's constructor.
		modelRunner.learn(model, train);
		//    f. To get more fancy for source code, we can convert the model into a complex mix model.
		//       - First wrap in a nested model, causing it to learn all files in test into a new 'local' model
		model = new NestedModel(modelRunner, test, model);
		//       - Then, add an ngram-cache component.
		//         * Note, order matters! The most local model should be "right-most" so it is called upon last (i.e. "gets the final say")
		//         * This is also why we apply the cache after the nested model
		model = new InverseMixModel(model, new CacheModel());
		//       - Finally, enable dynamic updating for the whole mixture (won't affect cache; it's dynamic by default)
		model.setDynamic(true);
		
		// 4. Running
		//    a. Model each file in 'test' recursively
		Stream<Pair<File, List<List<Double>>>> modeledFiles = modelRunner.model(model, test);
		//    b. Retrieve entropy statistics by mapping the entropies per file
		DoubleSummaryStatistics statistics = modelRunner.getStats(modeledFiles);
		
		System.out.printf("Modeled %d tokens, average entropy:\t%.4f\n", statistics.getCount(), statistics.getAverage());
	}
}
