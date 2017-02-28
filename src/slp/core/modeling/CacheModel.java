package slp.core.modeling;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import slp.core.counting.Counter;
import slp.core.modeling.ngram.NGramModel;
import slp.core.util.Configuration;
import slp.core.util.Pair;

public class CacheModel extends NGramModel {

	public static final int DEFAULT_CAPACITY = 5000;
	
	private final int capacity;
	private Model model;
	
	private Deque<List<Integer>> cache;
	static boolean open;
	
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
		this(capacity, counter, Model.standard(counter));
	}

	public CacheModel(int capacity, Counter counter, Model model) {
		super(counter);
		this.capacity = capacity;
		this.cache = new ArrayDeque<List<Integer>>(this.capacity);
		this.model = model;
		open = true;
	}

	@Override
	public Pair<Double, Double> modelWithConfidence(List<Integer> in) {
		Pair<Double, Double> prob = this.model.modelWithConfidence(in);
		if (open && in.size() == Configuration.order()) updateCache(in);
		return prob;
	}
	
	@Override
	protected Map<Integer, List<Pair<Double, Double>>> predictWithConfidence(List<Integer> in, Set<Integer> dejavu, int limit) {
		open = false;
		Map<Integer, List<Pair<Double, Double>>> prediction = this.model.predictWithConfidence(in, dejavu, limit);
		open = true;
		return prediction;
	}

	void updateCache(List<Integer> sequence) {
		if (this.capacity == 0) return;
		this.cache.addLast(sequence);
		this.counter.addForward(sequence);
		if (this.cache.size() > this.capacity) {
			List<Integer> removed = this.cache.removeFirst();
			this.counter.removeForward(removed);
		}
	}
}
