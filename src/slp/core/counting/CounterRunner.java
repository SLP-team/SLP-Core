package slp.core.counting;

import java.io.File;
import java.io.IOException;

import slp.core.sequences.SequenceRunner;

public class CounterRunner {
	
	private final SequenceRunner sequenceRunner;
	private final Counter counter;
	
	public CounterRunner() {
		this(new SequenceRunner());
	}
	
	public CounterRunner(SequenceRunner sequenceRunner) {
		this(sequenceRunner, new TrieGramCounter(sequenceRunner.getVocabulary()));
	}
	
	public CounterRunner(Counter counter) {
		this(new SequenceRunner(), counter);
	}
	
	public CounterRunner(SequenceRunner sequenceRunner, Counter counter) {
		this.sequenceRunner = sequenceRunner;
		this.counter = counter;
	}
	
	public SequenceRunner getSequenceRunner() {
		return sequenceRunner;
	}

	public Counter getCounter() {
		return counter;
	}

	public void map(File file) throws IOException {
		this.sequenceRunner.flatMap(file)
			.forEachOrdered(x -> {
				for (int i = 0; i < x.length; i++) {
					this.counter.update(x, i);
				}
			});
	}
}
