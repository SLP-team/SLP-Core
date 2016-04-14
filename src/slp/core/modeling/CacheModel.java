package slp.core.modeling;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.Counter;

public class CacheModel implements Model {

	public static final int DEFAULT_CAPACITY = 2000;
	
	private final int capacity;
	private final Counter counter;
	private final Model model;
	
	private Deque<List<Integer>> cache;
	
	public CacheModel() {
		this(DEFAULT_CAPACITY);
	}

	public CacheModel(int capacity) {
		this(capacity, Counter.standard());
	}

	public CacheModel(Counter counter) {
		this(DEFAULT_CAPACITY, counter);
	}

	public CacheModel(int capacity, Counter counter) {
		this(capacity, counter, new SimpleModel(counter));
	}

	public CacheModel(int capacity, Counter counter, Model model) {
		this.capacity = capacity;
		this.cache = new ArrayDeque<List<Integer>>(this.capacity);
		this.counter = counter;
		this.model = model;
	}
	
	@Override
	public double model(Stream<Integer> in) {
		List<Integer> sequence = in.collect(Collectors.toList());
		double prob = this.model.model(sequence.stream());
		updateCache(sequence);
		return prob;
	}

	private void updateCache(List<Integer> sequence) {
		this.cache.addLast(sequence);
		if (this.cache.size() > this.capacity) {
			List<Integer> removed = this.cache.removeFirst();
			this.counter.remove(removed.stream());
		}
		// Defer counting the just-modeled sequence to avoid adding a seen context for an unseen new event
		if (this.cache.size() > 1) {
			this.counter.add(this.cache.getFirst().stream());
		}
	}

}
