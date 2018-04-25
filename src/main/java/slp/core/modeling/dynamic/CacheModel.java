package slp.core.modeling.dynamic;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import slp.core.modeling.AbstractModel;
import slp.core.modeling.Model;
import slp.core.modeling.ngram.NGramModel;
import slp.core.util.Pair;

public class CacheModel extends AbstractModel {

	public static final int DEFAULT_CAPACITY = 5000;
	private final int capacity;
	
	private Model model;
	private final Deque<Pair<List<Integer>, Integer>> cache;
	private final Map<Integer, List<Integer>> cachedRefs;

	public CacheModel() {
		this(DEFAULT_CAPACITY);
	}
	
	public CacheModel(Model model) {
		this(model, DEFAULT_CAPACITY);
	}

	public CacheModel(int capacity) {
		this(NGramModel.standard(), capacity);
	}

	public CacheModel(Model model, int capacity) {
		this.model = model;
		// A cache is dynamic by default and only acts statically in prediction tasks
		setDynamic(true);
		
		this.capacity = capacity;
		this.cache = new ArrayDeque<>(this.capacity);
		this.cachedRefs = new HashMap<>();
	}
	
	@Override
	public void notify(File next) {
		try {
			this.model = this.model.getClass().getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			this.model = NGramModel.standard();
		}
		this.cache.clear();
		this.cachedRefs.clear();
	}

	// The cache model cannot be taught new events, it only learns after modeling
	@Override
	public void learn(List<Integer> input) { }
	@Override
	public void learnToken(List<Integer> input, int index) { }
	@Override
	public void forget(List<Integer> input) { }
	@Override
	public void forgetToken(List<Integer> input, int index) { }

	@Override
	public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
		Pair<Double, Double> modeled = this.model.modelToken(input, index);
		updateCache(input, index);
		return modeled;
	}

	private void updateCache(List<Integer> input, int index) {
		if (this.capacity > 0 && this.dynamic) {
			store(input, index);
			this.model.learnToken(input, index);
			if (this.cache.size() > this.capacity) {
				Pair<List<Integer>, Integer> removed = this.cache.removeFirst();
				this.model.forgetToken(removed.left, removed.right);
			}
		}
	}

	private void store(List<Integer> input, int index) {
		int hash = input.hashCode();
		List<Integer> list = this.cachedRefs.get(hash);
		if (list == null) {
			list = new ArrayList<>(input);
			this.cachedRefs.put(hash, list);
		}
		this.cache.addLast(Pair.of(list, index));
	}

	@Override
	public Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index) {
		return this.model.predictToken(input, index);
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
