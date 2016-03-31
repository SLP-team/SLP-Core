package slp.core.counting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import slp.core.util.Configuration;

public class FastCounterRunner extends CounterRunner {
	
	public FastCounterRunner() {
		super(new FastTrieGramCounter());
	}
	
	public void map(File file) throws IOException {
		this.getSequenceRunner().map(file)
			.forEachOrdered(x -> countLine(x));
	}

	private void countLine(Stream<Integer[]> line) {
		boolean[] b = {false};
		List<Integer[]> stored = new ArrayList<Integer[]>();
		line.forEachOrdered(x -> {
			b[0] |= x.length == Configuration.order();
			if (b[0]) this.getCounter().update(x);
			else stored.add(x);
		});
		if (!b[0]) {
			for (Integer[] x : stored) {
				this.getCounter().update(x);
			}
		}
	}
}
