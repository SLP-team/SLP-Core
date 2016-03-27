package slp.counting.core;

import java.util.stream.Stream;

public interface Counter {
	
	public void count(Stream<Integer[]> words);

	public void count(Integer[] words);
	
	public CounterProps getCounts();
}
