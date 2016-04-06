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

public class SimpleRunner {
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		File trainFile = new File("../java/temp");
		File testFile = new File("../java/temp2");
		double log2 = -Math.log(2);
		Tokenizer tokenizer = Tokenizer.standard();
		Sequencer sequencer = Sequencer.standard();

		Vocabulary vocabulary = new Vocabulary();
		Counter counter = Counter.standard();
		Reader.readLines(trainFile)
			.map(tokenizer::tokenize)
			.flatMap(sequencer::sequence)
			.map(vocabulary::toIndices)
			.forEachOrdered(counter::addBackwards);

		System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024/1024);
//		readWrite(vocabulary, counter);
//		Pair<Vocabulary, Counter> read = CountsReader.read(new File("counter.out"));
//		Vocabulary vocabulary = read.left;
//		Counter counter = read.right;
		Model model = Model.standard(counter);
		double prob = Reader.readLines(testFile)
			.map(tokenizer::tokenize)
			.flatMap(sequencer::sequence)
			.map(vocabulary::toIndices)
			.mapToDouble(model::model)
			.map(x -> Math.log(x)/log2)
			.average().orElse(0.0);
		System.out.println(prob);
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
