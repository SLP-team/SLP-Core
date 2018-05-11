package slp.core.example;

import java.io.File;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.giga.GigaCounter;
import slp.core.lexing.Lexer;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.misc.ReverseModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;

/**
 * This example shows how to make a bi-directional {@link Model} and what kinds of benefits this can have.
 * We use a {@link ReverseModel} wrapper and a {@link MixModel} to achieve this result.
 * We also use this occasion to show of running multiple models in parallel (forward, reverse and bi-directional model).
 * Although it doesn't train much faster (counting is already parallel with {@link GigaCounter}),
 * inference is more than twice as fast this way.
 * <br /><br />
 * Although bi-directional models aren't always realistic (for instance if you are trying to simulate left-to-right writing of code),
 * they can be substantially more robust for tasks like fault-localization, where the whole file is available.
 * See the {@link BasicJavaRunner} for a more detailed getting-started guide.
 * 
 * @author Vincent Hellendoorn
 */
public class ParallelBidirectional {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Please provide a train (and optional test) file/directory for this example");
			return;
		}
		File train = new File(args[0]);
		File test = args.length < 2 ? train : new File(args[1]);

		Lexer lexer = new JavaLexer();  // Use a Java lexer; if your code is already lexed, use whitespace or tokenized lexer
		LexerRunner lexerRunner = new LexerRunner(lexer, false);  // Don't model lines in isolation for code files.
		lexerRunner.setSentenceMarkers(true);  // Add start and end markers to the files
		lexerRunner.setExtension("java");  // We only lex Java files
		
		Vocabulary vocabulary = new Vocabulary();  // Make an empty vocabulary

		/*
		 * We will make three different models: a forward model, reverse model and a bi-direcitonal mixture of these
		 * and run all three of them in parallel. The results for all three will be printed at the end.
		 * The bidirectional model just mixes the forward and backward models here.
		 * 
		 * Please note that we don't use cache components; if you want those, you should give the bidirectional model
		 * its own forward and backward components (which normally you'd do anyways) so the cache doesn't get messed up.
		 */
		
		Model forward = new JMModel(6, new GigaCounter());  // Standard smoothing for code, giga-counter for large corpora
		Model backward = new ReverseModel(new JMModel(6, new GigaCounter()));  // Same, but wrapped in reverser
		Model bidirectional = MixModel.standard(forward, backward); // A bi-directional mixture of these
		
		// Make a model runner for each model. Note that you should set 'selfTesting' if the test data is the train data
		ModelRunner forwardRunner = new ModelRunner(forward, lexerRunner, vocabulary);
		ModelRunner backwardRunner = new ModelRunner(backward, lexerRunner, vocabulary);
		ModelRunner bidirectionalRunner = new ModelRunner(bidirectional, lexerRunner, vocabulary);
		
		// We only need to train the forwards and backwards models. Let's do it in parallel
		Stream.of(forwardRunner, backwardRunner).parallel()
			.forEach(r -> r.learnDirectory(train));  // In my setting, this makes training slightly faster
		
		// Let's test the three models in parallel as well, mapping them to their test statistics
		List<DoubleSummaryStatistics> statistics = Stream.of(forwardRunner, backwardRunner, bidirectionalRunner)
				.parallel()
				.map(runner -> runner.getStats(runner.modelDirectory(test)))
				.collect(Collectors.toList());  // In my setting, this more than doubles testing speed
		
		System.out.println("Total tokens: " + statistics.get(0).getCount());
		System.out.printf("Forward model entropy: %.4f\n", statistics.get(0).getAverage());
		System.out.printf("Backward model entropy: %.4f\n", statistics.get(1).getAverage());
		System.out.printf("Bidirectional model entropy: %.4f\n", statistics.get(2).getAverage());
	}
}
