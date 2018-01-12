package slp.core.modeling;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import slp.core.modeling.ngram.NGramModel;
import slp.core.sequencing.NGramSequencer;
import slp.core.util.Pair;

public class CacheModel extends AbstractModel {

	public static final int DEFAULT_CAPACITY = 5000;
	private final int capacity;
	
	private Model model;
	private final Deque<List<Integer>> cache;

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
		this.cache = new ArrayDeque<List<Integer>>(this.capacity);
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
	}

	private Map<List<Integer>, Set<Integer>> cached = new HashMap<>();
	@Override
	public void learnToken(List<Integer> input, int index) {
		if (this.capacity == 0) return;
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index);
		store(sequence);
		this.model.learnToken(sequence, sequence.size() - 1);
		if (this.cache.size() > this.capacity) {
			List<Integer> removed = this.cache.removeFirst();
			this.model.forgetToken(removed, removed.size() - 1);
		}
	}

	private void store(List<Integer> sequence) {
		this.cache.addLast(new ArrayList<>(sequence));
	}

	@Override
	public void learn(List<Integer> input) { }
	@Override
	public void forget(List<Integer> input) { }
	@Override
	public void forgetToken(List<Integer> input, int index) { }


	@Override
	public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
		return this.model.modelToken(input, index);
	}

	@Override
	public Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index) {
		return this.model.predictToken(input, index);
	}
}
