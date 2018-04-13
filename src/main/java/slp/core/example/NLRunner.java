package slp.core.example;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.modeling.Model;
import slp.core.modeling.ngram.ADMModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;
import slp.core.util.Pair;

public class NLRunner {
	public static void main(String[] args) {
		if (args.length < 1) return;
		// Assumes at least one argument, the path (file or directory) to train on
		File train = new File(args[0]);
		// If second argument, will test on that path, else will 'self-test' on train using full cross-validation per line
		File test = args.length < 2 ? train : new File(args[1]);
		
		// 1. Lexing
		//   a. Set up lexer using a PunctuationLexer (splits preserving punctuation, discarding whitespace,
		//      and preserving <unk>, <s> and </s> tokens). Could also use WhitespaceLexer (just splits on whitespace)
		Lexer lexer = new PunctuationLexer();
		LexerRunner lexerRunner = new LexerRunner(lexer);
		//   b. We will lex and model each line separately (often applicable, though not necessary for NLP)
		lexerRunner.setPerLine(true);
		//   c. Add start-of-line/end-of-line delimiters (to each line because perLine is set above)
		lexerRunner.setSentenceMarkers(true);
		
		
		// 2. Vocabulary
		//    a. Drop any events seen less than twice (i.e. one time) in training data
		//       (other values may be better, esp. for very larger corpora)
		VocabularyRunner.cutOff(2);
		//	  b. Build Vocabulary on train data, using VocabularyRunner.
		//       - Could use VocabularyRunner.write(file); to write this vocabulary for future use here
		Vocabulary vocabulary = VocabularyRunner.build(lexer, train);
		//    c. Close vocabulary (i.e. treat new words as "<unk>") now that it is complete.
		//		 - Note: this is typical for natural language, but less applicable to source code.
		vocabulary.close();
		
		
		// 3. Model
		//	  a. We will use an n-gram model with Modified Absolute Discounting (works well for NLP)
		//		 - The n-gram order is set to 4, which works better for NLP than the code standard (6) 
		Model model = new ADMModel(4);
		//       - We pass this Model to a ModelRunner, which takes care of most of the work and configuration for us
		ModelRunner modelRunner = new ModelRunner(lexerRunner, model);
		//    b. Self-testing if train is equal to test; this will un-count each file before modeling it.
		modelRunner.setSelfTesting(train.equals(test));
		//    c. We train this model on all files in 'train' recursively, using the usual updating mechanism (same as for dynamic updating).
		//       - Note that this invokes Model.learn for each file, which is fine for n-gram models since these are count-based;
		//          other models may prefer to pre-train when calling the Model's constructor.
		modelRunner.learn(train);
		
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
