package slp.core.runners;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.io.Reader;
import slp.core.modeling.Model;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;

public class RecursiveRunner {

	private static File trainDir = new File("E:/CP/GJava");
	private static File testDir = new File("E:/CP/GJava/1datapoint");
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		RecursiveRunner runner = new RecursiveRunner();
		runner.count(trainDir, true);
		System.out.println(runner.model(testDir).average().orElse(0.0));
	}
	
	private final Tokenizer tokenizer;
	private final Vocabulary vocabulary;
	private final Sequencer sequencer;
	private final Counter counter;
	private final Model model;
	
	public RecursiveRunner() throws ClassNotFoundException, IOException {
		this.tokenizer = Tokenizer.standard();
		this.vocabulary= Vocabulary.fromFile(new File(trainDir.getParentFile(), "all.vocab"));
		this.sequencer = Sequencer.standard();
		this.counter = Counter.standard();
		this.model = Model.standard(this.counter);
	}

	public void count(File file, boolean add) throws AccessDeniedException {
		if (file.getName().endsWith(".java") && file.isFile()) {
			Stream<List<Integer>> sequences = Stream.of(Reader.readContent(file))
				.map(this.tokenizer::tokenize)
				.map(this.vocabulary::toIndices)
				.flatMap(this.sequencer::sequenceForward);
			if (add) {
				sequences.forEachOrdered(this.counter::addForward);
			}
			else sequences.forEachOrdered(this.counter::removeForward);
		}
		else if (file.isDirectory()) {
			if (file.getParentFile().equals(trainDir)) {
				System.out.println(file + "\t" + this.counter.getCount() + "\t" + this.counter.getDistinctSuccessors());
			}
			for (String child : file.list()) {
				File childFile = new File(file, child);
				count(childFile, add);
			}
		}
	}

	private static final double log2 = Math.log(2);
	public DoubleStream model(File file) {
		DoubleStream entropies = DoubleStream.empty();
		if (file.getName().endsWith(".java") || !file.isDirectory()) {
			DoubleStream fileEntropies = Stream.of(Reader.readContent(file))
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
				try {
					entropies = DoubleStream.concat(entropies, model(childFile));
				} catch (NullPointerException e) {
					continue;
				}
			}
		}
		return entropies;
	}
}