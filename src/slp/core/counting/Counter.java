package slp.core.counting;

import java.util.stream.Stream;

public interface Counter {

	public Vocabulary getVocabulary();
	public void setVocabulary(Vocabulary vocabulary);
	public CounterProps<? extends Counter> counts();
	
	public void update(Stream<Integer[]> indexStream, boolean count);
	public default void update(Stream<Integer[]> indexStream) {
		update(indexStream, true);
	}
	
	public void update(Integer[] indices, boolean count);
	public default void update(Integer[] indices) {
		update(indices, true);
	}
	
	public void update(Integer[] indices, boolean count, int startIndex);
	public default void update(Integer[] indices, int startIndex) {
		update(indices, true, startIndex);
	}
	
	public void update(Integer[] indices, boolean count, int startIndex, int endIndex);
	public default void update(Integer[] indices, int startIndex, int endIndex) {
		update(indices, true, startIndex, endIndex);
	}
}
