package slp.core.modeling;

import java.util.stream.Stream;

public interface Model {

	public Stream<Double> model(Stream<Integer[]> in);

	public double model(Integer[] in);
	
}
