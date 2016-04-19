package slp.core.runners;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.counting.io.CountsReader;
import slp.core.counting.io.CountsWriter;
import slp.core.modeling.Model;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Token;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Pair;
import slp.core.util.Reader;

public class ExampleRunnerNL {

	public static long timeA = 0;
	public static long times[] = {0, 0, 0, 0, 0, 0};
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		Vocabulary vocabulary = new Vocabulary();
		Counter counter = Counter.standard();
		Model model = Model.standard(counter);
		for (int i = 1; i <= 5; i++) {
			System.out.print(i + "\n");
			times[0] -= System.currentTimeMillis();
			train(vocabulary, counter, new File("E:/LMCorpus/train2/" + (i < 10 ? "0" : "") + i));
			times[0] += System.currentTimeMillis();
			System.out.println(Arrays.toString(times));
			double entropy = test(vocabulary, model, new File("E:/LMCorpus/test2/00"));
			System.out.println(entropy);
		}
		System.out.println(counter.getCount() + "\t" + counter.getDistinctSuccessors());
	}

	private static void train(Vocabulary vocabulary, Counter counter, File trainFile) {
		Tokenizer tokenizer = Tokenizer.standard();
		Sequencer sequencer = Sequencer.standard();
		Reader.readLines(trainFile)
			.map(x -> {
				times[1] -= System.currentTimeMillis();
				Stream<Token> tokenize = tokenizer.tokenize(x);
				times[1] += System.currentTimeMillis();
				return tokenize;
			})
			.map(x -> {
				times[2] -= System.currentTimeMillis();
				Stream<Integer> indices = vocabulary.toIndices(x);
				times[2] += System.currentTimeMillis();
				return indices;
			})
			.flatMap(x -> {
				times[3] -= System.currentTimeMillis();
				Stream<List<Integer>> sequenceForward = sequencer.sequenceForward(x);
				times[3] += System.currentTimeMillis();
				return sequenceForward;
			})
			.forEachOrdered(t -> {
				times[4] -= System.currentTimeMillis();
				counter.addForward(t);
				times[4] += System.currentTimeMillis();
			});
	}

	private static double test(Vocabulary vocabulary, Model model, File testFile) {
		final double log2 = -Math.log(2);
		Tokenizer tokenizer = Tokenizer.standard();
		Sequencer sequencer = Sequencer.standard();
		
		return Reader.readLines(testFile)
			.limit(250)
			.map(tokenizer::tokenize)
			.map(vocabulary::toIndices)
			.map(sequencer::sequenceBackward)
			.flatMap(x -> x.skip(1))
			.mapToDouble(model::model)
			.map(x -> Math.log(x)/log2)
			.average().orElse(0.0);
	}

	@SuppressWarnings("unused")
	private static void readWrite(Vocabulary vocabulary, Counter counter) throws IOException {
		try {
			File file = new File("counter.out");
			CountsWriter.write(vocabulary, counter, file);
			Pair<Vocabulary, Counter> read = CountsReader.read(file);
			vocabulary = read.left;
			counter = read.right;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
