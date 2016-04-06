package slp.core.counting;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Abandon all hope, ye who enter here.
 * Seriously, this class needs some refactoring. It's designed as a parody of TrieGramCounter that does not incur
 * as heavy an object overhead (notably, small hashmaps) and is quite effective at reducing memory cost.
 * 
 * @author Vincent
 *
 */
public class TrieGramCounterHackArray implements Counter {

	public static final int DEFAULT_INDEX = -1;

	private int index;
	private int count;
	
	private int nature = -1;

	private static final int ARRAY_INIT_SIZE = 1;
	private static final int ARRAY_MAX_SIZE = 32;
	private TrieGramCounterHackArray[] successorArray = null;
	
	private Map<Integer, TrieGramCounterHackArray> successors;
	private Multimap<Integer, TrieGramCounterHackArray> countsMap;

	public TrieGramCounterHackArray() {
		this(DEFAULT_INDEX);
	}

	public TrieGramCounterHackArray(int index) {
		this(index, 0);
	}

	public TrieGramCounterHackArray(int index, int expectedSize) {
		this.index = index;
		this.successorArray = new TrieGramCounterHackArray[ARRAY_INIT_SIZE];
		updateNature(expectedSize);
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
	public TrieGramCounterHackArray getSuccessor(Integer key) {
		switch (this.nature) {
			case 0: return getSuccessor0(key);
			case 1: return getSuccessor1(key);
		}
		return null;
	}

	private TrieGramCounterHackArray getSuccessor0(Integer key) {
		for (TrieGramCounterHackArray counter : this.successorArray) {
			if (counter == null) return null;
			else if (counter.getIndex() == key) return counter;
		}
		return null;
	}

	private TrieGramCounterHackArray getSuccessor1(Integer key) {
		return this.successors == null ? null : this.successors.get(key);
	}

	public final TrieGramCounterHackArray getOrPutSuccessor(Integer key) {
		TrieGramCounterHackArray trieGramCounter = getSuccessor(key);
		if (trieGramCounter != null) {
			return trieGramCounter;
		}
		else {
			switch (this.nature) {
				case 0: return putSuccessor0(key);
				case 1: return putSuccessor1(key);
			}
			return null;
		}
	}

	private TrieGramCounterHackArray putSuccessor0(Integer key) {
		int length = this.successorArray.length;
		if (this.successorArray[length - 1] != null) {
			if (length < ARRAY_MAX_SIZE) {
				this.successorArray = Arrays.copyOf(this.successorArray, 2*length);
				return putSuccessor0(key);
			}
			else {
				setNewNature(1);
				return putSuccessor1(key);
			}
		}
		for (int i = 0; i < length; i++) {
			if (this.successorArray[i] == null) {
				this.successorArray[i] = new TrieGramCounterHackArray(key);
				return this.successorArray[i];
			}
		}
		// Should never happen
		return null;
	}

	private TrieGramCounterHackArray putSuccessor1(Integer key) {
		TrieGramCounterHackArray counter = new TrieGramCounterHackArray(key);
		this.successors.put(key, counter);
		return counter;
	}

	private void removeSuccessor(int index) {
		switch (this.nature) {
			case 0: removeSuccessor0(index); break;
			case 1: removeSuccessor1(index); break;
		}	
	}

	private void removeSuccessor0(int index) {
		for (int i = 0; i < this.successorArray.length; i++) {
			if (this.successorArray[i].getIndex() == this.index) {
				int j = i + 1;
				for (; j < this.successorArray.length; j++) {
					if (this.successorArray[j] == null) break;
					this.successorArray[j - 1] = this.successorArray[j];
				}
				this.successorArray[this.successorArray.length - 1] = null;
				return;
			}
		}
	}

	private void removeSuccessor1(int index) {
		this.successors.remove(this.index);
		updateNature(this.successors.size());
	}

	/*
	 * Code to update the counts tree with new sequences
	 */
	@Override
	public void update(Stream<Integer> indices, boolean count, int startIndex) {
		TrieGramCounterHackArray[] path = getOrCreatePath(indices, startIndex);
		TrieGramCounterHackArray last = path[path.length - 1];
		last.updateCount(count);
		path[0].updateCount(count);
		path[path.length - 2].updateMaps(last, count);
	}

	@Override
	public void updateForward(Stream<Integer> indices, boolean count, int startIndex) {
		TrieGramCounterHackArray[] path = getOrCreatePath(indices, startIndex);
		for (int i = 0; i < path.length; i++) {
			TrieGramCounterHackArray counter = path[i];
			counter.updateCount(count);
			if (i > 0) {
				path[i - 1].updateMaps(counter, count);
			}
		}
	}

	private void updateMaps(TrieGramCounterHackArray successor, boolean added) {
		if (this.nature != 1) return;
		// Update new count stats
		int count = successor.getCount();
		if (count != 0) this.countsMap.put(count, successor);
		else removeSuccessor(successor.getIndex());
		// Update previous count
		int old = count + (added ? -1 : 1);
		if (old != 0) {
			this.countsMap.remove(old, successor);
			if (this.countsMap.get(old).isEmpty()) this.countsMap.removeAll(old);
		}
	}

	/*
	 * Getters and setters related to sequence counts
	 */
	@Override
	public int getDistinctSuccessors() {
		switch (this.nature) {
			case 0: return getDistinctSuccessors0();
			case 1: return getDistinctSuccessors1();
		}
		return 0;
	}

	private int getDistinctSuccessors0() {
		for (int i = 0; i < this.successorArray.length; i++) {
			if (this.successorArray[i] == null) return i;
		}
		return this.successorArray.length;
	}

	private int getDistinctSuccessors1() {
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
		TrieGramCounterHackArray[] counters = getPath(temp.stream(), startIndex);
		TrieGramCounterHackArray counter = counters[counters.length - 1];
		TrieGramCounterHackArray context = counters[counters.length - 2];
		counts[0] = counter == null ? 0 : counter.getCount();
		counts[1] = context.getCount();
		return counts;
	}

	@Override
	public int[] getDistinctCounts(int range, Stream<Integer> indices, int startIndex) {
		int[] distinctCounts = new int[range];
		TrieGramCounterHackArray[] counters = getPath(indices, startIndex);
		TrieGramCounterHackArray counter = counters[counters.length - 1];
		int overallCount = counter.countsMap.size();
		for (int i = 1; i < range; i++) {
			int countI = counter.countsMap.get(i).size();
			distinctCounts[i] = countI;
			overallCount -= countI;
		}
		distinctCounts[range - 1] = overallCount;
		return distinctCounts;
	}

	private TrieGramCounterHackArray[] getOrCreatePath(Stream<Integer> indices, int startIndex) {
		List<TrieGramCounterHackArray> path = new ArrayList<TrieGramCounterHackArray>();
		indices.skip(startIndex)
			.forEachOrdered(x -> extendPath(x, path, true));
		return path.toArray(new TrieGramCounterHackArray[path.size()]);
	}

	private TrieGramCounterHackArray[] getPath(Stream<Integer> indices, int startIndex) {
		List<TrieGramCounterHackArray> path = new ArrayList<TrieGramCounterHackArray>();
		indices.skip(startIndex)
			.forEachOrdered(x -> extendPath(x, path, false));
		return path.toArray(new TrieGramCounterHackArray[path.size()]);
	}

	private void extendPath(Integer x, List<TrieGramCounterHackArray> path, boolean grow) {
		if (path.isEmpty()) path.add(this);
		TrieGramCounterHackArray prev = path.get(path.size() - 1);
		if (prev == null) return;
		TrieGramCounterHackArray next;
		if (grow) next = prev.getOrPutSuccessor(x);
		else next = prev.getSuccessor(x);
		path.add(next);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.index);
		out.writeInt(this.count);
		switch (this.nature) {
			case 1: loadArray(); break;
			default: break;
		}
		out.writeInt(this.successors.size());
		for (Entry<Integer, TrieGramCounterHackArray> entry : this.successors.entrySet()) {
			out.writeInt(entry.getKey());
			entry.getValue().writeExternal(out);
		}
	}

	private void loadArray() {
		initTreeStructure();
		Arrays.stream(this.successorArray)
			.forEachOrdered(x -> this.successors.put(x.getIndex(), x));
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
		if (size <= ARRAY_INIT_SIZE) {
			this.successorArray = new TrieGramCounterHackArray[ARRAY_INIT_SIZE];
			for (int i = 0; i < size; i++) {
				this.successorArray[i] = (TrieGramCounterHackArray) in.readObject();
			}
		}
		else {
			this.successors = new HashMap<Integer, TrieGramCounterHackArray>(size, 1.0f);
			for (int i = 0; i < size; i++) {
				int index = in.readInt();
				TrieGramCounterHackArray counter = new TrieGramCounterHackArray();
				counter.readExternal(in);
				this.successors.put(index, counter);
			}
		}
	}

	private void initCountsMap() {
		if (this.successors != null) {
			this.countsMap = HashMultimap.create();
			for (TrieGramCounterHackArray counter : this.successors.values()) {
				this.countsMap.put(counter.getCount(), counter);
			}
		}
	}

	/*
	 * Functionality for switching/updating the nature of this counter
	 */
	private void updateNature(int count) {
		if (count > 0.75*ARRAY_INIT_SIZE) {
			if (this.nature != 1) setNewNature(1);
		} else {
			if (this.nature != 0) setNewNature(0);
		}
	}

	private void setNewNature(int newNature) {
		switch (this.nature) {
			case 0: updateNature0(newNature); break;
			case 1: updateNature1(newNature); break;
		}
		this.nature = newNature;
	}

	private void updateNature0(int newNature) {
		if (newNature == 0) return;
		else {
			initTreeStructure();
			Arrays.stream(this.successorArray)
				.forEachOrdered(x -> this.successors.put(x.getIndex(), x));
			this.successorArray = null;
		}
	}

	private void updateNature1(int newNature) {
		if (newNature == 1) return;
		else {
			int length = ARRAY_MAX_SIZE;
			while (length/2 > ARRAY_INIT_SIZE && length/2 > this.successors.size()) length /= 2;
			this.successorArray = new TrieGramCounterHackArray[length];
			int i = 0;
			for (TrieGramCounterHackArray x : this.successors.values()) {
				this.successorArray[i++] = x;
			}
			this.successors = null;
			this.countsMap = null;
		}
	}
	
	private void initTreeStructure() {
		if (this.successors == null) {
			this.successors = new HashMap<Integer, TrieGramCounterHackArray>();
			this.countsMap = HashMultimap.create();	
		}
	}
}
