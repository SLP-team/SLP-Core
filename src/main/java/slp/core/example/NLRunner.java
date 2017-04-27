package slp.core.example;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Stream;

import slp.core.lexing.LexerRunner;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.ngram.ADMModel;
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
		LexerRunner.setLexer(new PunctuationLexer());
		//   b. Add start-of-line/end-of-line delimiters (to each file, or to each line if perLine is set as below)
		LexerRunner.addSentenceMarkers(true);
		//   c. Set delimiters for each line separately (often applicable, though not necessary for NLP)
		//      - Note that this does not imply or exclude modeling per file; that must explicitly set as below.
		//      - There are cases in which we want to add per-line delimiters but still model a whole file as one.
		LexerRunner.perLine(true);
		
		// 2. Vocabulary
		//    a. Omit any events seen less than twice (i.e. one time) in training data
		//       (other values may be better, esp. for very larger corpora)
		VocabularyRunner.cutOff(2);
		//    b. Close vocabulary after building it (typical for NLP, less applicable to source code).
		VocabularyRunner.close(true);
		//    c. Build on train data.
		//       - Could use VocabularyRunner.write(file); to write this vocabulary for future use here
		VocabularyRunner.build(train);
		
		// 3. Model
		//    a. Model each line in isolation (typical for NLP; again, not linked to LexerRunner.perLine)
		ModelRunner.perLine(true);
		//    b. Self-testing if train is equal to test; will un-count each file before modeling it.
		ModelRunner.selfTesting(train.equals(test));
		//    c. Set n-gram model order, 4 works well for NLP
		ModelRunner.setNGramOrder(4);
		//    d. Use an n-gram model with modified absolute discounting smoothing
		Model model = new ADMModel();
		//    e. Train this model on all files in 'train' recursively, using the usual updating mechanism (same as for dynamic updating).
		//       - Note that this invokes Model.learn for each file, which is fine for n-gram models since these are count-based;
		//          other models may prefer to pre-train when calling the Model's constructor.
		ModelRunner.learn(model, train);
		
		// 4. Running
		//    a. Model each file in 'test' recursively
		Stream<Pair<File, List<List<Double>>>> modeledFiles = ModelRunner.model(model, test);
		//    b. Retrieve entropy statistics by mapping the entropies per file
		DoubleSummaryStatistics statistics = modeledFiles.map(pair -> pair.right)
			// Note the "skip(1)" (per line), since we added delimiters and we generally don't model the start-of-line token
			.flatMap(f -> f.stream().flatMap(l -> l.stream().skip(1)))
			.mapToDouble(d -> d)
			.summaryStatistics();
		
		System.out.printf("Modeled %d tokens, average entropy:\t%.4f\n", statistics.getCount(), statistics.getAverage());
	}
}
