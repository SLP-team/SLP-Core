package slp.core.modeling;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.CounterRunner;
import slp.core.sequences.SequenceRunner;

public class ModelRunner {

	private final SequenceRunner sequenceRunner;
	private final Model model;

	public ModelRunner(Counter counter) {
		this(new SequenceRunner(), new SimpleModel(counter));
	}
	
	public ModelRunner(CounterRunner counterRunner) {
		this(new SequenceRunner(), new SimpleModel(counterRunner.getCounter()));
	}
	
	public ModelRunner(Model model) {
		this(new SequenceRunner(), model);
	}
	
	public ModelRunner(SequenceRunner sequenceRunner, Model model) {
		this.sequenceRunner = sequenceRunner;
		this.model = model;
	}
	
	public Stream<Double> map(File file) throws IOException {
		return this.sequenceRunner.flatMap(file)
			.map(this.model::model);
	}
}
