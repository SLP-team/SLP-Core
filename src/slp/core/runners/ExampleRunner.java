package slp.core.runners;

import java.io.File;
import java.io.IOException;

import slp.core.counting.CounterRunner;
import slp.core.counting.FastCounterRunner;
import slp.core.modeling.ModelRunner;

public class ExampleRunner {
	
	public static void main(String[] args) throws IOException {
		File file = new File(args.length > 0 ? args[0] : "../java/temp");
		CounterRunner counterRunner = new FastCounterRunner();
		counterRunner.map(file);
		ModelRunner modelRunner = new ModelRunner(counterRunner);
		double log2 = -Math.log(2);
		double prob = modelRunner.map(file)
				.mapToDouble(x -> Math.log(x)/log2)
				.average().orElse(0.0);
		System.out.println(prob);
	}
}
