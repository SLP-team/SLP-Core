package slp.core.modeling.ngram;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import slp.core.counting.trie.TrieCounter;
import slp.core.modeling.AbstractModel;
import slp.core.sequencing.NGramSequencer;
import slp.core.util.Pair;

public class NGramCache extends AbstractModel {

	public static final int DEFAULT_CAPACITY = 5000;
	private final int capacity;
	
	private NGramModel model;
	private final Deque<List<Integer>> cache;
	
	public NGramCache() {
		this(DEFAULT_CAPACITY);
	}

	public NGramCache(int capacity) {
		this.model = NGramModel.standard();
		// A cache is dynamic by default and only acts statically in prediction tasks
		setDynamic(true);
		
		this.capacity = capacity;
		this.cache = new ArrayDeque<List<Integer>>(this.capacity);
	}
	
	@Override
	public void notify(File next) {
		this.model = NGramModel.standard(new TrieCounter());
		this.cache.clear();
	}

	@Override
	public void learnToken(List<Integer> input, int index) {
		if (this.capacity == 0) return;
		List<Integer> sequence = NGramSequencer.sequenceAt(input, index);
		this.cache.addLast(sequence);
		for (int i = 0; i < sequence.size(); i++) this.model.counter.count(sequence.subList(i, sequence.size()));
		if (this.cache.size() > this.capacity) {
			List<Integer> removed = this.cache.removeFirst();
			for (int i = 0; i < removed.size(); i++) this.model.counter.unCount(removed.subList(i, removed.size()));
		}
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
