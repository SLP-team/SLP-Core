package slp.core.runners;

import java.io.File;
import java.io.IOException;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.io.Reader;
import slp.core.modeling.Model;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;

public class ExampleRunnerCode {
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		File trainFile = new File("../java/fold0.train");
		File testFile = new File("../java/fold1.train");
		double log2 = -Math.log(2);
		Tokenizer tokenizer = Tokenizer.standard();
		Sequencer sequencer = Sequencer.standard();
		Vocabulary vocabulary = new Vocabulary();
		Counter counter = Counter.standard();
		
		long t = System.currentTimeMillis();
		Reader.readLines(trainFile)
			.map(x -> "<s> " + x + " </s>")
			.map(tokenizer::tokenize)
			.map(vocabulary::toIndices)
			.flatMap(sequencer::sequenceForward)
			.forEachOrdered(counter::addForward);
		vocabulary.close();
		
		System.out.println(counter.getCount() + "\t" + counter.getDistinctSuccessors());
		System.out.println((System.currentTimeMillis() - t)/1000 + "\t" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024/1024);
		Model model = Model.standard(counter);
		double prob = Reader.readLines(testFile)
			.map(x -> "<s> " + x + " </s>")
			.map(tokenizer::tokenize)
			.map(vocabulary::toIndices)
			.flatMap(sequencer::sequenceFull)
			.mapToDouble(model::model)
			.map(x -> Math.log(x)/log2)
			.average().orElse(0.0);
		System.out.println(prob);
	}
}
