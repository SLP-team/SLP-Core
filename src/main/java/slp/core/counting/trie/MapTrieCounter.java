package slp.core.counting.trie;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import slp.core.util.Pair;

public class MapTrieCounter extends AbstractTrie {
	/**
	 * 'counts' contains in order: own count, context count (sum of successor's counts),
	 * no of distinct successors seen once, twice, up to the COCcutoff in Configuration
	 */
	private Int2ObjectMap<Object> map;

	// Maximum depth in trie to use Map-tries, after this regular Tries are used, which are slower but more memory-efficient
	private static final int MAX_DEPTH_MAP_TRIE = 1;
	
	public MapTrieCounter() {
		this(1);
	}

	public MapTrieCounter(int initSize) {
		super();
		this.map = new Int2ObjectOpenHashMap<>(initSize);
	}
	
	@Override
	public List<Integer> getTopSuccessors(List<Integer> indices, int limit) {
		return this.map.int2ObjectEntrySet().stream()
			.map(e -> Pair.of(e.getIntKey(), getCount(e.getValue())))
			.sorted((p1, p2) -> -Integer.compare(p1.right, p2.right))
			.limit(limit)
			.map(p -> p.left)
			.collect(Collectors.toList());
	}

	@Override
	AbstractTrie makeNext(int depth) {
		AbstractTrie newNext;
		if (depth <= MAX_DEPTH_MAP_TRIE) newNext = new MapTrieCounter(1);
		else newNext = new TrieCounter();
		return newNext;
	}


	@Override
	Object getSuccessor(int next) {
		return this.map.get(next);
	}

	@Override
	void putSuccessor(int next, Object o) {
		this.map.put(next, o);
	}

	@Override
	void removeSuccessor(int next) {
		this.map.remove(next);
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
				if (code < -1) value = new TrieCounter();
				else value = new MapTrieCounter();
				((AbstractTrie) value).readExternal(in);
				this.counts[1 + Math.min(((AbstractTrie) value).getCount(), COUNT_OF_COUNTS_CUTOFF)]++;
			}
			else {
				value = new int[code];
				for (int j = 0; j < code; j++) ((int[]) value)[j] = in.readInt();
				this.counts[1 + Math.min(((int[]) value)[0], COUNT_OF_COUNTS_CUTOFF)]++;
			}
			this.map.put(key, value);
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
				if (o instanceof TrieCounter) out.writeInt(-2);
				else out.writeInt(-1);
				((AbstractTrie) o).writeExternal(out);
			}
		}
	}
}
