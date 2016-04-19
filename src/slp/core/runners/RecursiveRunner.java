package slp.core.runners;

import java.io.File;
import java.io.IOException;
import java.util.stream.DoubleStream;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.modeling.Model;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Reader;

public class RecursiveRunner {
	
	public static void main(String[] args) throws IOException {
		File trainDir = new File("../java2/fulldirs/batik");
		File testDir = new File("../java2/fulldirs/ant");
		RecursiveRunner runner = new RecursiveRunner();
		runner.count(trainDir);
		System.out.println(runner.counter.getCount() + "\t" + runner.counter.getDistinctSuccessors());
		double entropy = runner.model(testDir)
			.average().orElse(0.0);
		System.out.println(entropy);
	}
	
	private final Tokenizer tokenizer;
	private final Vocabulary vocabulary;
	private final Sequencer sequencer;
	private final Counter counter;
	private final Model model;
	
	public RecursiveRunner() {
		this.tokenizer = Tokenizer.standard();
		this.vocabulary= Vocabulary.create();
		this.sequencer = Sequencer.standard();
		this.counter = Counter.standard();
		this.model = Model.standard(this.counter);
	}

	public void count(File directory) {
		for (String child : directory.list()) {
			if (directory.getName().equals("fulldirs"))
				System.out.println(child);
			File file = new File(directory, child);
			if (file.isDirectory()) {
				count(file);
			}
			else {
				countFile(file);
			}
			if (directory.getName().equals("fulldirs"))
				System.out.println(this.counter.getCount());
		}
	}

	private void countFile(File file) {
		Reader.readLines(file)
			.map(this.tokenizer::tokenize)
			.map(this.vocabulary::toIndices)
			.flatMap(this.sequencer::sequenceForward)
			.forEachOrdered(this.counter::addForward);
	}

	public DoubleStream model(File directory) {
		DoubleStream entropies = DoubleStream.empty();
		for (String child : directory.list()) {
			File file = new File(directory, child);
			if (file.isDirectory()) {
				entropies = DoubleStream.concat(entropies, model(file));
			}
			else {
				entropies = DoubleStream.concat(entropies, modelFile(file));
			}
		}
		return entropies;
	}

	private DoubleStream modelFile(File file) {
		double log2 = Math.log(2);
		return Reader.readLines(file)
			.map(this.tokenizer::tokenize)
			.map(this.vocabulary::toIndices)
			.flatMap(this.sequencer::sequenceBackward)
			.mapToDouble(this.model::model)
			.map(x -> -Math.log(x)/log2);
	}
}
