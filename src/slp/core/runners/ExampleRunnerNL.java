package slp.core.runners;

import java.io.File;
import java.io.IOException;
import java.util.stream.DoubleStream;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.counting.io.CountsReader;
import slp.core.counting.io.CountsWriter;
import slp.core.io.Reader;
import slp.core.modeling.Model;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Pair;

public class ExampleRunnerNL {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String root = (args.length > 0 ? args[0] : "E:/LMCorpus/");
		File trainRoot = new File(root + "train");
		File testRoot = new File(root + "test");
		Vocabulary vocabulary = Vocabulary.fromFile(new File(root + "vocab.out"));
		Counter	counter = Counter.standard();
		Model model = Model.standard(counter);
		for (int i = 1; i < 100; i++) {
			System.out.print(i + "\t");
			long t = System.currentTimeMillis();
			train(vocabulary, counter, new File(trainRoot, (i < 10 ? "0" : "") + i));
			long timeTrain = System.currentTimeMillis() - t;
			int count = counter.getCount();
			int vocab = counter.getDistinctSuccessors();
			System.out.print(count + "\t" + vocab + "\t");
			t = System.currentTimeMillis();
			double entropy = test(testRoot, vocabulary, model);
			long timeTest = System.currentTimeMillis() - t;
			System.out.println(entropy + "\t" + timeTrain + "\t" + timeTest);
		}
	}

	private static void train(Vocabulary vocabulary, Counter counter, File trainFile) {
		Tokenizer tokenizer = Tokenizer.standard();
		Sequencer sequencer = Sequencer.standard();
		Reader.readLines(trainFile)
			.map(tokenizer::tokenize)
			.map(vocabulary::toIndices)
			.flatMap(sequencer::sequenceForward)
			.forEachOrdered(counter::addForward);
	}

	private static double test(File testRoot, Vocabulary vocabulary, Model model) {
		DoubleStream result = DoubleStream.empty();
		for (int i = 0; i < 1; i++) {
			DoubleStream test = test(vocabulary, model, new File(testRoot, (i < 10 ? "0" : "") + i));
			result = DoubleStream.concat(result, test);
		}
		return result.average().orElse(0.0);
	}

	private static DoubleStream test(Vocabulary vocabulary, Model model, File testFile) {
		final double log2 = -Math.log(2);
		Tokenizer tokenizer = Tokenizer.standard();
		Sequencer sequencer = Sequencer.standard();
		return Reader.readLines(testFile)
			.map(tokenizer::tokenize)
			.map(vocabulary::toIndices)
			.map(sequencer::sequenceBackward)
			.flatMap(x -> x)
			.mapToDouble(model::model)
			.map(x -> Math.log(x)/log2);
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
