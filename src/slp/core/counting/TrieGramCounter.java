package slp.core.counting;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class TrieGramCounter implements Counter {

	public static final int DEFAULT_INDEX = -1;

	private int index;
	private int count;
	private Map<Integer, TrieGramCounter> successors;
	private Multimap<Integer, TrieGramCounter> countsMap;

	public TrieGramCounter() {
		this(DEFAULT_INDEX);
	}

	public TrieGramCounter(int index) {
		this.index = index;
	}

	public TrieGramCounter(int index, int count, Map<Integer, TrieGramCounter> successors,
			Multimap<Integer, TrieGramCounter> countsMap) {
		this.index = index;
		this.count = count;
		this.successors = successors;
		this.countsMap = countsMap;
	}

	private void init() {
		if (this.successors == null) {
			this.successors = new HashMap<Integer, TrieGramCounter>();
			this.countsMap = HashMultimap.create();	
		}
	}

	public int getIndex() {
		return this.index;
	}

	/*
	 * Getters and setters related to count
	 */
	@Override
	public int getCount() {
		return this.count;
	}
	
	public void updateCount(boolean count) {
		if (count) inc(); else dec();
	}

	private void inc() {
		this.count++;
	}
	
	private void dec() {
		this.count--;
	}

	/*
	 * Getters and setters related to successors
	 */
	public Map<Integer, TrieGramCounter> getSuccessors() {
		if (this.successors == null) init();
		return this.successors;
	}
	
	public TrieGramCounter getSuccessor(Integer key) {
		return this.successors == null ? null : this.successors.get(key);
	}

	public final TrieGramCounter getOrPutSuccessor(Integer key) {
		init();
		TrieGramCounter trieGramCounter = getSuccessor(key);
		if (trieGramCounter == null) {
			trieGramCounter = new TrieGramCounter(key);
			putSuccessor(key, trieGramCounter);
		}
		return trieGramCounter;
	}

	private void putSuccessor(Integer key, TrieGramCounter trieGramCounter) {
		this.successors.put(key, trieGramCounter);
	}

	/*
	 * Getters and setters related to the counts map
	 */
	public Multimap<Integer, TrieGramCounter> getCountsMap() {
		if (this.countsMap == null) init();
		return this.countsMap;
	}

	public Collection<TrieGramCounter> getCountsSet(Integer key) {
		return this.countsMap == null ? null : this.countsMap.get(key);
	}
	
	public void putCounts(Integer key, TrieGramCounter trieGramCounter) {
		getCountsMap().put(key, trieGramCounter);
	}

	/*
	 * Code to update the counts tree with new sequences
	 */
	@Override
	public void update(Stream<Integer> indices, boolean count, int startIndex) {
		TrieGramCounter[] path = getOrCreatePath(indices, startIndex);
		TrieGramCounter last = path[path.length - 1];
		last.updateCount(count);
		path[0].updateCount(count);
		path[path.length - 2].updateCountsMap(last, count);
	}

	@Override
	public void updateForward(Stream<Integer> indices, boolean count, int startIndex) {
		TrieGramCounter[] path = getOrCreatePath(indices, startIndex);
		for (int i = 0; i < path.length; i++) {
			TrieGramCounter counter = path[i];
			counter.updateCount(count);
			if (i > 0) {
				path[i - 1].updateCountsMap(counter, count);
			}
		}
	}

	private void updateCountsMap(TrieGramCounter successor, boolean added) {
		Multimap<Integer, TrieGramCounter> countsMap = getCountsMap();
		int count = successor.getCount();
		int old = count + (added ? -1 : 1);
		if (count != 0) countsMap.put(count, successor);
		if (old != 0) countsMap.remove(count, successor);
	}

	/*
	 * Getters and setters related to sequence counts
	 */
	@Override
	public int getDistinctSuccessors() {
		return this.successors.size();
	}

	@Override
	public int[][] getFullCounts(Stream<Integer> indices, int startIndex) {
		List<Integer> sequence = indices.collect(Collectors.toList());
		int[][] counts = new int[2][sequence.size()];
		for (int i = 0; i < sequence.size() - 1; i++) {
			int offset = sequence.size() - i - 1;
			int[] shortCounts = getShortCounts(sequence.stream(), i);
			counts[0][offset] = shortCounts[0];
			counts[1][offset] = shortCounts[1];
		}
		return counts;
	}
	
	@Override
	public int[] getShortCounts(Stream<Integer> indices, int startIndex) {
		int[] counts = new int[2];
		List<Integer> temp = indices.collect(Collectors.toList());
		TrieGramCounter[] counters = getPath(temp.stream(), startIndex);
		TrieGramCounter counter = counters[counters.length - 1];
		TrieGramCounter context = counters[counters.length - 2];
		counts[0] = counter == null ? 0 : counter.getCount();
		counts[1] = context.getCount();
		return counts;
	}

	@Override
	public int[] getDistinctCounts(int range, Stream<Integer> indices, int startIndex) {
		int[] distinctCounts = new int[range];
		TrieGramCounter[] counters = getPath(indices, startIndex);
		TrieGramCounter counter = counters[counters.length - 1];
		Multimap<Integer, TrieGramCounter> counts = counter.getCountsMap();
		int overallCount = counts.size();
		for (int i = 1; i < range; i++) {
			int countI = counts.get(i).size();
			distinctCounts[i] = countI;
			overallCount -= countI;
		}
		distinctCounts[range - 1] = overallCount;
		return distinctCounts;
	}

	private TrieGramCounter[] getOrCreatePath(Stream<Integer> indices, int startIndex) {
		List<TrieGramCounter> path = new ArrayList<TrieGramCounter>();
		indices.skip(startIndex)
			.forEachOrdered(x -> extendPath(x, path, true));
		return path.toArray(new TrieGramCounter[path.size()]);
	}

	private TrieGramCounter[] getPath(Stream<Integer> indices, int startIndex) {
		List<TrieGramCounter> path = new ArrayList<TrieGramCounter>();
		indices.skip(startIndex)
			.forEachOrdered(x -> extendPath(x, path, false));
		return path.toArray(new TrieGramCounter[path.size()]);
	}

	private void extendPath(Integer x, List<TrieGramCounter> path, boolean grow) {
		if (path.isEmpty()) path.add(this);
		TrieGramCounter prev = path.get(path.size() - 1);
		if (prev == null) return;
		TrieGramCounter next;
		if (grow) next = prev.getOrPutSuccessor(x);
		else next = prev.getSuccessor(x);
		path.add(next);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.index);
		out.writeInt(this.count);
		if (this.successors != null) {
			out.writeInt(this.successors.size());
			for (Entry<Integer, TrieGramCounter> entry : this.successors.entrySet()) {
				out.writeInt(entry.getKey());
				entry.getValue().writeExternal(out);
			}
		}
		else {
			out.writeInt(0);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.index = in.readInt();
		this.count = in.readInt();
		readSuccessors(in);
		initCountsMap();
	}

	private void readSuccessors(ObjectInput in) throws ClassNotFoundException, IOException {
		int size = in.readInt(); 
		if (size == 0) return;
		this.successors = new HashMap<Integer, TrieGramCounter>(size, 1.0f);
		for (int i = 0; i < size; i++) {
			int index = in.readInt();
			TrieGramCounter counter = new TrieGramCounter();
			counter.readExternal(in);
			this.successors.put(index, counter);
		}
	}

	private void initCountsMap() {
		if (this.successors != null) {
			this.countsMap = HashMultimap.create();
			for (TrieGramCounter counter : this.successors.values()) {
				this.countsMap.put(counter.getCount(), counter);
			}
		}
	}
}
