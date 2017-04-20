package slp.core.example;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Stream;

import slp.core.lexing.LexerRunner;
import slp.core.lexing.code.JavaLexer;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.mix.InverseMixModel;
import slp.core.modeling.mix.NestedModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.ngram.NGramCache;
import slp.core.translating.VocabularyRunner;
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
		LexerRunner.setLexer(new JavaLexer());
		//   b. Do not tokenize per line for Java (invoked for the sake of example; false is the default)
		LexerRunner.perLine(false);
		//   c. But, do add delimiters (to start and end of file)
		LexerRunner.useDelimiters(true);
		
		
		// 2. Vocabulary
		//    a. Do not cut-off any counts (default)
		VocabularyRunner.cutOff(0);
		//    b. Do not close after building (also default)
		VocabularyRunner.close(false);
		//    c. Build on train data.
		//       - Since we are not modifying the defaults in any way, we could also just let the vocabulary be built while training;
		//       - Building first yields the same result, but could e.g. allow one to write the vocabulary before training.
		VocabularyRunner.build(train);
		
		
		// 3. Model
		//    a. No per line modeling for Java (default)
		ModelRunner.perLine(false);
		//    b. Self-testing if train is equal to test; will un-count each file before modeling it.
		ModelRunner.selfTesting(train.equals(test));
		//    c. Set n-gram model order, 6 works well for Java
		ModelRunner.setNGramOrder(6);
		//    d. Use an n-gram model with simple Jelinek-Mercer smoothing (works well for code)
		Model model = new JMModel();
		
		//    e. To get more fancy for source code, we can convert the model into a complex mix model.
		//       - First wrap in a nested model
		model = new NestedModel(test, model);
		//       - Then, add an ngram-cache component.
		//         * Note, order matters! The most local model should be "right-most" so it is called upon last ("gets the final say")
		//         * This is also why we apply the cache after the nested model
		model = new InverseMixModel(model, new NGramCache());
		//       - Finally, enable dynamic updating for the whole mixture (won't affect cache; it's dynamic by default)
		model.setDynamic(true);
		
		//    f. Train this model on all files in 'train' recursively, using the usual updating mechanism (same as for dynamic updating).
		//       - Note that this invokes Model.learn for each file, which is fine for n-gram models since these are count-based;
		//          other models may prefer to pre-train when calling the Model's constructor.
		//       - Note that we could've invoked 'learn' before all the wrapping in this case
		ModelRunner.learn(model, train);
		
		// 4. Running
		//    a. Model each file in 'test' recursively
		Stream<Pair<File, List<List<Double>>>> modeledFiles = ModelRunner.model(model, test);
		//    b. Retrieve entropy statistics by mapping the entropies per file
		DoubleSummaryStatistics statistics = modeledFiles.map(pair -> pair.right)
			.flatMap(l -> l.stream()
			// Note the "skip(1)", since we added delimiters and we generally don't model the start-of-line token
			.flatMap(t -> t.stream().skip(1)))
			.mapToDouble(d -> d)
			.summaryStatistics();
		
		System.out.printf("Modeled %d tokens, average entropy:\t%.4f\n", statistics.getCount(), statistics.getAverage());
	}
}
