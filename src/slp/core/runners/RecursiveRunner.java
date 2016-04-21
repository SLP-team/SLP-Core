package slp.core.runners;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.modeling.Model;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Reader;

public class RecursiveRunner {

	private static File trainDir = new File("../java2/fulldirs/ant/org/apache");
	private static File testDir = new File("../java2/fulldirs/ant/org/apache");
	public static void main(String[] args) throws IOException {
		RecursiveRunner runner = new RecursiveRunner();
		runner.count(trainDir, true);
		System.out.println(runner.model(testDir).average().orElse(0.0));
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

	public void count(File file, boolean add) {
		if (file.getName().contains(".") || !file.isDirectory()) {
			Stream<List<Integer>> sequences = Reader.readLines(file)
				.map(x -> "<s> " + x + " </s>")
				.map(this.tokenizer::tokenize)
				.map(this.vocabulary::toIndices)
				.flatMap(this.sequencer::sequenceForward);
			if (add) sequences.forEachOrdered(this.counter::addForward);
			else sequences.forEachOrdered(this.counter::removeForward);
		}
		else {
			for (String child : file.list()) {
				File childFile = new File(file, child);
				count(childFile, add);
			}
		}
	}

	private static final double log2 = Math.log(2);
	public DoubleStream model(File file) {
		DoubleStream entropies = DoubleStream.empty();
		if (file.getName().contains(".") || !file.isDirectory()) {
			DoubleStream fileEntropies = Reader.readLines(file)
				.map(x -> "<s> " + x + " </s>")
				.map(this.tokenizer::tokenize)
				.map(this.vocabulary::toIndices)
				.flatMap(this.sequencer::sequenceBackward)
				.mapToDouble(this.model::model)
				.map(x -> -Math.log(x)/log2);
			entropies = DoubleStream.concat(entropies, fileEntropies);
		}
		else {
			for (String child : file.list()) {
				File childFile = new File(file, child);
				entropies = DoubleStream.concat(entropies, model(childFile));
			}
		}
		return entropies;
	}
}