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

/**
 * This example shows a typical use-case for natural language of this tool with detailed comments.
 * We setup a {@link LexerRunner} with a {@link Lexer}, train a {@link Model} using a {@link ModelRunner}
 * and print the overall result. This is a good starting point for understanding the tool's API.
 * See also the {@link BasicJavaRunner} for an equivalent example with typical settings for source code modeling tasks.
 * 
 * @author Vincent Hellendoorn
 */
public class BasicNLRunner {
	public static void main(String[] args) {
		if (args.length < 1) return;
		// Assumes at least one argument, the path (file or directory) to train on
		File train = new File(args[0]);
		// If second argument, will test on that path, else will 'self-test' on train using full cross-validation per line
		File test = args.length < 2 ? train : new File(args[1]);
		
		// 1. Lexing
		//   a. Set up lexer using a PunctuationLexer (splits on whitespace, and separates out punctuation).
		//	    - You can also use WhitespaceLexer (just splits on whitespace), for instance if you already lexed your text.
		//		- The second parameter informs it that we want to treat each line in the file in isolation.
		//		  This is often true for natural language tasks, but change it if applicable for you.
		Lexer lexer = new PunctuationLexer();
		LexerRunner lexerRunner = new LexerRunner(lexer, true);
		//   b. If your data does not contain sentence markers (for the start and end of each file), add these here;
		//		- The model will assume that these markers are present and always skip the first token when modeling
		lexerRunner.setSentenceMarkers(true);
		
		
		// 2. Vocabulary
		//    a. Ignore any words seen less than twice (i.e. one time) in training data, replacing these with "<unk>"
		//       (other values may be better, esp. for very larger corpora)
		VocabularyRunner.cutOff(2);
		//	  b. Build the vocabulary on the training data with convenience function provided by VocabularyRunner
		//       - You can use VocabularyRunner.write to write it for future use (VocabularyRunner.read to read it back in)
		Vocabulary vocabulary = VocabularyRunner.build(lexerRunner, train);
		//    c. Close the resulting vocabulary (i.e. treat new words as "<unk>") now that it is complete.
		//		 - Note: this is typical for natural language, but less applicable to source code.
		vocabulary.close();
		
		
		// 3. Model
		//	  a. We will use an n-gram model with Modified Absolute Discounting (works well for NLP)
		//		 - The n-gram order is set to 4, which works better for NLP than the code standard (6)
		//		 - See the JavaRunner for examples on making this model dynamic;
		//		   this is not typical for NLP tasks but can be very useful depending on your data
		Model model = new ADMModel(4);
		//	  b. We create a ModelRunner with this model and ask it to learn the train directory
		//		 - This invokes Model.learn for each file, which is fine for n-gram models since these are count-based;
		//         other model implementations may prefer to train in their own way.
		ModelRunner modelRunner = new ModelRunner(model, lexerRunner, vocabulary);
		modelRunner.learnDirectory(train);
		//    d. We assume you are self-testing if the train and test directory are equal.
		//		 This will make it temporarily forget any sequence to model, effectively letting you train and test on all your data
		modelRunner.setSelfTesting(train.equals(test));
		
		
		// 4. Running
		//    a. Model each file in 'test' recursively
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
