package slp.core.example;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import slp.core.counting.giga.GigaCounter;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.lexing.simple.CharacterLexer;
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
public class CharacterWordHybrid {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Please provide a train (and optional test) file/directory for this example");
			return;
		}
		args[0] += "/AccelService";
		File train = new File(args[0]);
		File test = args.length < 2 ? train : new File(args[1]);

		LexerRunner wordLexerRunner = new LexerRunner(new JavaLexer(), false);
		LexerRunner charLexerRunner = new LexerRunner(new CharacterLexer(), false);

		ModelRunner wordRunner = new ModelRunner(new JMModel(6, new GigaCounter()), wordLexerRunner, new Vocabulary());
		ModelRunner charRunner = new ModelRunner(new JMModel(10), charLexerRunner, new Vocabulary());

		wordRunner.learnDirectory(train);
		charRunner.learnDirectory(train);

		List<List<String>> words = wordLexerRunner.lexFile(test)
				.map(l -> l.collect(Collectors.toList())).collect(Collectors.toList());
		List<List<String>> chars = charLexerRunner.lexFile(test)
				.map(l -> l.collect(Collectors.toList())).collect(Collectors.toList());
		List<List<Double>> wordProbs = wordRunner.modelFile(test);
		List<List<Double>> charProbs = charRunner.modelFile(test);
		
		for (int i = 0; i < words.size(); i++) {
			System.out.printf("Line %d", i + 1);
			for (int j = 0; j < words.get(i).size(); j++) {
				System.out.printf(" %s: %.4f", words.get(i).get(j), wordProbs.get(i).get(j));
			}
			System.out.println();
			System.out.print("\t");
			for (int j = 0; j < chars.get(i).size(); j++) {
				System.out.printf(" %s: %.4f", chars.get(i).get(j), charProbs.get(i).get(j));
			}
			System.out.println();
		}
	}
}
