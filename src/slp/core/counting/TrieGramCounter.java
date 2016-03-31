package slp.core.counting;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Stream;

import com.google.common.collect.TreeMultimap;

public class TrieGramCounter implements Counter, Comparable<TrieGramCounter> {

	public static final int DEFAULT_INDEX = -1;

	private static Vocabulary vocabulary;
	private TrieGramCounterProps props;
	private final int index;
	private int count;

	private final Map<Integer, TrieGramCounter> successors;
	private final TreeMultimap<Integer, TrieGramCounter> counts;
	
	public TrieGramCounter() {
		this(DEFAULT_INDEX);
	}

	public TrieGramCounter(Vocabulary vocabulary) {
		this(DEFAULT_INDEX, vocabulary);
	}

	public TrieGramCounter(int index) {
		if (vocabulary == null) vocabulary = new Vocabulary();
		this.props = new TrieGramCounterProps(this);
		this.index = index;
		this.count = 0;
		
		this.successors = new HashMap<Integer, TrieGramCounter>();
		this.counts = TreeMultimap.create();
	}

	public TrieGramCounter(int index, Vocabulary vocabulary) {
		this(index);
		TrieGramCounter.vocabulary = vocabulary;
	}

	@Override
	public Vocabulary getVocabulary() {
		return null;
	}

	@Override
	public void setVocabulary(Vocabulary vocabulary) {
		TrieGramCounter.vocabulary = vocabulary;
	}

	@Override
	public CounterProps<TrieGramCounter> counts() {
		return this.props;
	}

	public int getCount() {
		return this.count;
	}

	public void inc() {
		this.count++;
	}
	
	public void dec() {
		this.count--;
	}

	public int getIndex() {
		return this.index;
	}
	
	public Map<Integer, TrieGramCounter> getSuccessors() {
		return this.successors;
	}
	
	public TreeMultimap<Integer, TrieGramCounter> getCounts() {
		return this.counts;
	}
	
	public TrieGramCounter getSuccessor(Integer key) {
		return this.successors.get(key);
	}
	
	public SortedSet<TrieGramCounter> getCounts(Integer key) {
		return this.counts.get(key);
	}

	@Override
	public void update(Stream<Integer[]> indexStream, boolean count) {
		indexStream.forEachOrdered(x -> this.update(x, count));
	}

	@Override
	public void update(Integer[] indices, boolean count) {
		this.update(indices, count, 0);
	}

	@Override
	public void update(Integer[] indices, boolean count, int startIndex) {
		this.update(indices, count, startIndex, indices.length);
	}

	@Override
	public void update(Integer[] indices, boolean count, int startIndex, int endIndex) {
		if (startIndex > endIndex) {
			System.err.println("Invalid start/end index " + startIndex + "/" + endIndex);
			return;
		}
		if (this.index == -1 || startIndex == endIndex) {
			if (count) inc();
			else dec();
		}
		if (startIndex < endIndex) {
			Integer atI = indices[startIndex];
			TrieGramCounter successor = getOrPutSuccessor(atI);
			if (startIndex == endIndex - 1) {
				this.counts.get(successor.getCount()).remove(successor);
			}
			successor.update(indices, count, startIndex + 1, endIndex);
			if (startIndex == endIndex - 1 && successor.getCount() > 0) {
				this.counts.get(successor.getCount()).add(successor);
			}
		}
	}

	@Override
	public int compareTo(TrieGramCounter o) {
		int countCompare = -Integer.compare(this.getCount(), o.getCount());
		if (countCompare != 0)
			return countCompare;
		else
			return Integer.compare(this.getIndex(), o.getIndex());
	}

	private final TrieGramCounter getOrPutSuccessor(Integer key) {
		TrieGramCounter trieGramCounter = this.successors.get(key);
		if (trieGramCounter == null) {
			trieGramCounter = new TrieGramCounter(key);
			this.successors.put(key, trieGramCounter);
		}
		return trieGramCounter;
	}
}
