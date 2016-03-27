package slp.modeling.core;

import java.util.stream.Stream;

public interface Model {

	public Stream<Double> model(Stream<Integer[]> in);

	public Stream<Double> model(Integer[] in);
	
}
