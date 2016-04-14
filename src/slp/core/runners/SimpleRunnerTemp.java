package slp.core.runners;

import java.io.File;
import java.io.IOException;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.counting.io.CountsReader;
import slp.core.counting.io.CountsWriter;
import slp.core.modeling.Model;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Pair;
import slp.core.util.Reader;

public class SimpleRunnerTemp {
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		Vocabulary vocabulary = new Vocabulary();
		Counter counter = Counter.standard();
		Model model = Model.standard(counter);
		
		train(vocabulary, counter, new File("E:/LMCorpus/train/news.en-00001-of-00100"));
//		train(vocabulary, counter, new File("E:/LMCorpus/train/news.en-00002-of-00100"));
//		train(vocabulary, counter, new File("E:/LMCorpus/train/news.en-00003-of-00100"));
//		train(vocabulary, counter, new File("E:/LMCorpus/train/news.en-00004-of-00100"));
//		train(vocabulary, counter, new File("E:/LMCorpus/train/news.en-00005-of-00100"));
//		train(vocabulary, counter, new File("E:/LMCorpus/train/news.en-00006-of-00100"));
//		train(vocabulary, counter, new File("E:/LMCorpus/train/news.en-00007-of-00100"));
		double entropy = test(vocabulary, model, new File("E:/LMCorpus/test/news.en.heldout-00000-of-00050"));
		System.out.println(entropy);
	}

	private static void train(Vocabulary vocabulary, Counter counter, File trainFile) {
		System.out.println("Training " + trainFile.getName() + " ... ");
		Tokenizer tokenizer = Tokenizer.standard();
		Sequencer sequencer = Sequencer.standard();

		Reader.readLines(trainFile)
			.map(x -> "<s> " + x + "</s>")
			.map(tokenizer::tokenize)
			.flatMap(sequencer::sequence)
			.map(vocabulary::toIndices)
			.forEachOrdered(counter::addBackwards);
	}
	
	private static double test(Vocabulary vocabulary, Model model, File testFile) {
		final double log2 = -Math.log(2);
		Tokenizer tokenizer = Tokenizer.standard();
		Sequencer sequencer = Sequencer.standard();

		double prob = Reader.readLines(testFile)
				.map(x -> "<s> " + x + "</s>")
				.map(tokenizer::tokenize)
				.map(x -> x.skip(1))
				.flatMap(sequencer::sequence)
				.map(vocabulary::toIndices)
				.mapToDouble(model::model)
				.map(x -> Math.log(x)/log2)
				.average().orElse(0.0);
		return prob;
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
