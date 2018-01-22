package slp.core.counting.trie;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class MapTrieCounter extends AbstractTrie {
	/**
	 * 'counts' contains in order: own count, context count (sum of successor's counts),
	 * no of distinct successors seen once, twice, up to the COCcutoff in Configuration
	 */
	private Int2ObjectMap<Object> map;
	private IntList pseudoOrdering;

	// Maximum depth in trie to use Map-tries, after this Array-Tries are used, which are slower but more memory-efficient
	private static final int MAX_DEPTH_MAP_TRIE = 1;
	
	public MapTrieCounter() {
		this(1);
	}

	public MapTrieCounter(int initSize) {
		super();
		this.map = new Int2ObjectOpenHashMap<>(initSize);
		this.map.defaultReturnValue(null);
		this.pseudoOrdering = new IntArrayList();
	}

	@Override
	public List<Integer> getSuccessors() {
		return this.map.keySet().stream().collect(Collectors.toList());
	}
	
	private static Map<Integer, Integer> cache = new HashMap<>();
	@Override
	public List<Integer> getTopSuccessorsInternal(int limit) {
		int classKey = this.hashCode();
		int countsKey = this.keyCode();
		Integer cached = cache.get(classKey);
		if (cached == null || cached != countsKey) {
			Collections.sort(this.pseudoOrdering, (i1, i2) -> compareCounts(i1, i2));
		}
		int end = Math.min(this.pseudoOrdering.size(), limit);
		List<Integer> topSuccessors = new ArrayList<>(this.pseudoOrdering.subList(0, end));
		if (this.getSuccessorCount() > 10) cache.put(classKey, countsKey);
		return topSuccessors;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	private int keyCode() {
		return 31*(this.getSuccessorCount() + 31*this.getCount());
	}

	@Override
	AbstractTrie makeNext(int depth) {
		AbstractTrie newNext;
		if (depth <= MAX_DEPTH_MAP_TRIE) newNext = new MapTrieCounter(1);
		else newNext = new ArrayTrieCounter();
		return newNext;
	}

	@Override
	public Object getSuccessor(int next) {
		return this.map.get(next);
	}

	@Override
	void putSuccessor(int next, Object o) {
		Object curr = this.map.put(next, o);
		if (curr == null) this.pseudoOrdering.add(next);
	}

	private int compareCounts(Integer i1, Integer i2) {
		int base = -Integer.compare(getCount(this.map.get((int) i1)), getCount(this.map.get((int) i2)));
		if (base != 0) return base;
		return Integer.compare(i1, i2);
	}

	@Override
	void removeSuccessor(int next) {
		Object removed = this.map.remove(next);
		this.pseudoOrdering.rem(next);
		if (removed instanceof MapTrieCounter) {
			cache.remove(((MapTrieCounter) removed).hashCode());
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.counts = new int[2 + COUNT_OF_COUNTS_CUTOFF];
		this.counts[0] = in.readInt();
		this.counts[1] = in.readInt();
		int successors = in.readInt();
		this.map = new Int2ObjectOpenHashMap<>(successors, 0.9f);
		int pos = 0;
		for (; pos < successors; pos++) {
			int key = in.readInt();
			int code = in.readInt();
			Object value;
			if (code < 0) {
				if (code < -1) value = new ArrayTrieCounter();
				else value = new MapTrieCounter();
				((AbstractTrie) value).readExternal(in);
				this.counts[1 + Math.min(((AbstractTrie) value).getCount(), COUNT_OF_COUNTS_CUTOFF)]++;
			}
			else {
				value = new int[code];
				for (int j = 0; j < code; j++) ((int[]) value)[j] = in.readInt();
				this.counts[1 + Math.min(((int[]) value)[0], COUNT_OF_COUNTS_CUTOFF)]++;
			}
			this.putSuccessor(key, value);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.counts[0]);
		out.writeInt(this.counts[1]);
		out.writeInt(this.map.size());
		for (Entry<Integer, Object> entry : this.map.int2ObjectEntrySet()) {
			int key = entry.getKey();
			Object value = entry.getValue();
			out.writeInt(key);
			Object o = value;
			if (o instanceof int[]) {
				int[] arr = (int[]) o;
				out.writeInt(arr.length);
				for (int j = 0; j < arr.length; j++) out.writeInt(arr[j]);
			}
			else {
				if (o instanceof ArrayTrieCounter) out.writeInt(-2);
				else out.writeInt(-1);
				((AbstractTrie) o).writeExternal(out);
			}
		}
	}
}
