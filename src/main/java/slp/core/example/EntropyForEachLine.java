package slp.core.example;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

import slp.core.counting.giga.GigaCounter;
import slp.core.lexing.Lexer;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.dynamic.CacheModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;

/**
 * This example shows how to get the entropy for each line and for each token on each line in a test file.
 * It assumes you are passing it two arguments: a train file/directory and a test file,
 * both with Java code (but you can change that below).
 * You can replace the model here with a bi-directional model (if applicable) for better performance (see {@link ParallelBidirectional})
 * See the {@link BasicJavaRunner} for a more detailed getting-started guide.
 * 
 * @author Vincent Hellendoorn
 */
public class EntropyForEachLine {
	public static void main(String[] args) {
		if (args.length < 2 || !new File(args[1]).isFile()) {
			System.err.println("Please provide a train file/directory and a test file for this example");
			return;
		}
		File train = new File(args[0]);
		File test = new File(args[1]);

		Lexer lexer = new JavaLexer();  // Use a Java lexer; if your code is already lexed, use whitespace or tokenized lexer
		LexerRunner lexerRunner = new LexerRunner(lexer, false);  // Don't model lines in isolation for code files.
																  // We will still get per-line, per-token entropies
		lexerRunner.setSentenceMarkers(true);  // Add start and end markers to the files
		lexerRunner.setExtension("java");  // We only lex Java files

		Vocabulary vocabulary = new Vocabulary();  // Create an empty vocabulary
		
		Model model = new JMModel(6, new GigaCounter());  // Standard smoothing for code, giga-counter for large corpora
		model = MixModel.standard(model, new CacheModel());  // Use a simple cache model; see JavaRunner for more options
		ModelRunner modelRunner = new ModelRunner(model, lexerRunner, vocabulary); // Use above lexer and vocabulary
		modelRunner.learnDirectory(train);  // Teach the model all the data in "train"
		
		// Modeling one file gives us entropy per-line, per token in nested list. See also modelRunner.modelDirectory
		List<List<Double>> fileEntropies = modelRunner.modelFile(test);
		List<List<String>> fileTokens = lexerRunner.lexFile(test)  // Let's also retrieve the tokens on each line
				.map(l -> l.collect(Collectors.toList()))
				.collect(Collectors.toList());
		for (int i = 0; i < fileEntropies.size(); i++) {
			List<String> lineTokens = fileTokens.get(i);
			List<Double> lineEntropies = fileEntropies.get(i);
			
			// First use Java's stream API to summarize entropies on this line
			// (see modelRunner.getStats for summarizing file or directory results)
			DoubleSummaryStatistics lineStatistics = lineEntropies.stream()
					.mapToDouble(Double::doubleValue)
					.summaryStatistics();
			double averageEntropy = lineStatistics.getAverage();
			
			// Then, print out the average entropy and the entropy for every token on this line
			System.out.printf("Line %d, avg.: %.4f, tokens:", i + 1, averageEntropy);
			for (int j = 0; j < lineTokens.size(); j++) {
				System.out.printf(" %s: %.4f", lineTokens.get(j), lineEntropies.get(j));
			}
			System.out.println();
		}
	}
}
