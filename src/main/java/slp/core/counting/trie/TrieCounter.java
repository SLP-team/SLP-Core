package slp.core.counting.trie;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import slp.core.util.Pair;

public class TrieCounter extends AbstractTrie {

	public int[] indices;
	public Object[] successors;
	private static final double GROWTH_FACTOR = 1.5;

	public TrieCounter() {
		this(1);
	}
	
	public TrieCounter(int initSize) {
		super();
		this.indices = new int[initSize];
		this.successors = new Object[initSize];
		Arrays.fill(this.indices, Integer.MAX_VALUE);
	}
	
	@Override
	public List<Integer> getTopSuccessors(List<Integer> indices, int limit) {
		return IntStream.range(0, this.indices.length)
			.filter(i -> this.indices[i] != Integer.MAX_VALUE)
			.mapToObj(i -> Pair.of(this.indices[i], this.getCount(this.successors[i])))
			.filter(p -> p.right != null && p.right > 0)
			.sorted((p1, p2) -> -Integer.compare(p1.right, p2.right))
			.limit(limit)
			.map(p -> p.left)
			.collect(Collectors.toList());
	}

	@Override
	AbstractTrie makeNext(int depth) {
		return new TrieCounter();
	}
	
	Object getSuccessor(int key) {
		int ix = getSuccIx(key);
		if (ix < 0) {
			return null;
		}
		else return this.successors[ix];
	}

	void removeSuccessor(int index) {
		int ix = getSuccIx(index);
		if (ix >= 0) {
			if (ix < this.indices.length - 1) {
				System.arraycopy(this.indices, ix + 1, this.indices, ix, this.indices.length - ix - 1);
				System.arraycopy(this.successors, ix + 1, this.successors, ix, this.successors.length - ix - 1);
			}
			this.indices[this.indices.length - 1] = Integer.MAX_VALUE;
			int padding = getSuccIx(Integer.MAX_VALUE);
			if (padding >= 5 && padding < this.indices.length / 2) {
				this.indices = Arrays.copyOf(this.indices, padding + 1);
				this.successors = Arrays.copyOf(this.successors, padding + 1);
			}
		}
	}

	void putSuccessor(int key, Object o) {
		int ix = getSuccIx(key);
		if (ix >= 0) {
			this.successors[ix] = o;
		} else {
			ix = -ix - 1;
			if (ix >= this.indices.length) grow();
			if (this.indices[ix] != Integer.MAX_VALUE) {
				System.arraycopy(this.indices, ix, this.indices, ix + 1, this.indices.length - ix - 1);
				System.arraycopy(this.successors, ix, this.successors, ix + 1, this.successors.length - ix - 1);
			}
			this.indices[ix] = key;
			this.successors[ix] = o;
			if (this.indices[this.indices.length - 1] != Integer.MAX_VALUE) {
				grow();
			}
		}
	}

	/*
	 * Map bookkeeping
	 */
	private int getSuccIx(int key) {
		// Quickly check if key is stored at its purely sequential location; the 'root' trie is usually
		// populated with the whole vocabulary in order up to some point, so a quick guess can save time.
		if (this.indices.length > 1000 && key > 0 && key <= this.indices.length && this.indices[key - 1] == key) return key - 1;
		// Otherwise, binary search will do
		return Arrays.binarySearch(this.indices, key);
	}

	private void grow() {
		int oldLen = this.indices.length;
		int newLen = (int) (this.indices.length * GROWTH_FACTOR + 1);
		if (newLen == oldLen - 1) newLen++;
		this.indices = Arrays.copyOf(this.indices, newLen);
		this.successors = Arrays.copyOf(this.successors, newLen);
		for (int i = oldLen; i < this.indices.length; i++) this.indices[i] = Integer.MAX_VALUE;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.counts = new int[2 + COUNT_OF_COUNTS_CUTOFF];
		this.counts[0] = in.readInt();
		this.counts[1] = in.readInt();
		int successors = in.readInt();
		this.indices = new int[successors];
		this.successors = new Object[successors];
		int pos = 0;
		for (; pos < successors; pos++) {
			int key = in.readInt();
			int code = in.readInt();
			Object value;
			if (code < 0) {
				value = in.readObject();
				this.counts[1 + Math.min(((TrieCounter) value).getCount(), COUNT_OF_COUNTS_CUTOFF)]++;
			}
			else {
				value = new int[code];
				for (int j = 0; j < code; j++) ((int[]) value)[j] = in.readInt();
				this.counts[1 + Math.min(((int[]) value)[0], COUNT_OF_COUNTS_CUTOFF)]++;
			}
			this.indices[pos] = key;
			this.successors[pos] = value;
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.counts[0]);
		out.writeInt(this.counts[1]);
		out.writeInt(this.getSuccessorCount());
		for (int i = 0; i < this.indices.length; i++) {
			if (this.indices[i] == Integer.MAX_VALUE) continue;
			out.writeInt(this.indices[i]);
			Object o = this.successors[i];
			if (o instanceof int[]) {
				int[] arr = (int[]) o;
				out.writeInt(arr.length);
				for (int j = 0; j < arr.length; j++) out.writeInt(arr[j]);
			}
			else {
				out.writeInt(-1);
				out.writeObject((AbstractTrie) o);
			}
		}
	}
}
