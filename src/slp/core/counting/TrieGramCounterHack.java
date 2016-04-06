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
public class TrieGramCounterHack implements Counter {

	public static final int DEFAULT_INDEX = -1;

	// TODO: 2 bits nature, 2*8+6 (22) bits index, 4*8 (32) bits count
//	private byte[] value = new byte[7]
	
	private int index;
	private int count;
	
	private int nature = 0;
	
	private TrieGramCounterHack successor1 = null;
	private TrieGramCounterHack successor2 = null;
	private TrieGramCounterHack successor3 = null;
	
	private static final int ARRAY_SIZE = 16;
	private TrieGramCounterHack [] successorArray = null;
	
	private Map<Integer, TrieGramCounterHack> successors;
	private Multimap<Integer, TrieGramCounterHack> countsMap;

	public TrieGramCounterHack() {
		this(DEFAULT_INDEX);
	}

	public TrieGramCounterHack(int index) {
		this(index, 0);
	}

	public TrieGramCounterHack(int index, int expectedSize) {
		this.index = index;
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
	public TrieGramCounterHack getSuccessor(Integer key) {
		switch (this.nature) {
			case 0: return getSuccessor1(key);
			case 1: return getSuccessor2(key);
			case 2: return getSuccessor3(key);
		}
		return null;
	}

	private TrieGramCounterHack getSuccessor1(Integer key) {
		if (this.successor1 == null) return null;
		else if (this.successor1.getIndex() == key) return this.successor1;
		else if (this.successor2 == null) return null;
		else if (this.successor2.getIndex() == key) return this.successor2;
		else if (this.successor3 == null) return null;
		else if (this.successor3.getIndex() == key) return this.successor3;
		else return null;
	}

	private TrieGramCounterHack getSuccessor2(Integer key) {
		if (this.successorArray == null) return null;
		for (TrieGramCounterHack counter : this.successorArray) {
			if (counter == null) return null;
			else if (counter.getIndex() == key) return counter;
		}
		return null;
	}

	private TrieGramCounterHack getSuccessor3(Integer key) {
		return this.successors == null ? null : this.successors.get(key);
	}

	public final TrieGramCounterHack getOrPutSuccessor(Integer key) {
		TrieGramCounterHack trieGramCounter = getSuccessor(key);
		if (trieGramCounter != null) {
			return trieGramCounter;
		}
		else {
			switch (this.nature) {
				case 0: return putSuccessor1(key);
				case 1: return putSuccessor2(key);
				case 2: return putSuccessor3(key);
			}
			return null;
		}
	}

	private TrieGramCounterHack putSuccessor1(Integer key) {
		if (this.successor1 == null) {
			this.successor1 = new TrieGramCounterHack(key);
			return this.successor1;
		}
		else if (this.successor2 == null) {
			this.successor2 = new TrieGramCounterHack(key);
			return this.successor2;
		}
		else if (this.successor3 == null) {
			this.successor3 = new TrieGramCounterHack(key);
			return this.successor3;
		}
		else {
			setNewNature(1);
			return putSuccessor2(key);
		}
	}

	private TrieGramCounterHack putSuccessor2(Integer key) {
		if (this.successorArray[this.successorArray.length - 1] != null) {
			setNewNature(2);
			return putSuccessor3(key);
		}
		for (int i = 0; i < this.successorArray.length; i++) {
			if (this.successorArray[i] == null) {
				this.successorArray[i] = new TrieGramCounterHack(key);
				return this.successorArray[i];
			}
		}
		// Should never happen
		return null;
	}

	private TrieGramCounterHack putSuccessor3(Integer key) {
		TrieGramCounterHack counter = new TrieGramCounterHack(key);
		this.successors.put(key, counter);
		return counter;
	}

	private void removeSuccessor(int index) {
		switch (this.nature) {
			case 0: removeSuccessor1(index); break;
			case 1: removeSuccessor2(index); break;
			case 2: removeSuccessor3(index); break;
		}	
	}

	private void removeSuccessor1(int index2) {
		if (this.successor1.getIndex() == index2) {
			this.successor1 = this.successor3;
			this.successor3 = null;
		}
		else if (this.successor2.getIndex() == index2) {
			this.successor2 = this.successor3;
			this.successor3 = null;
		}
		else if (this.successor3.getIndex() == index2) {
			this.successor3 = null;
		}
	}

	private void removeSuccessor2(int index2) {
		for (int i = 0; i < this.successorArray.length; i++) {
			if (this.successorArray[i].getIndex() == this.index) {
				int j = i + 1;
				for (; j < this.successorArray.length; j++) {
					if (this.successorArray[j] == null) break;
					this.successorArray[j - 1] = this.successorArray[j];
				}
				if (j < 4) {
					setNewNature(0);
				}
				else {
					this.successorArray[this.successorArray.length - 1] = null;
				}
				return;
			}
		}
	}

	private void removeSuccessor3(int index2) {
		this.successors.remove(this.index);
		updateNature(this.successors.size());
	}

	/*
	 * Code to update the counts tree with new sequences
	 */
	@Override
	public void update(Stream<Integer> indices, boolean count, int startIndex) {
		TrieGramCounterHack[] path = getOrCreatePath(indices, startIndex);
		TrieGramCounterHack last = path[path.length - 1];
		last.updateCount(count);
		path[0].updateCount(count);
		path[path.length - 2].updateMaps(last, count);
	}

	@Override
	public void updateForward(Stream<Integer> indices, boolean count, int startIndex) {
		TrieGramCounterHack[] path = getOrCreatePath(indices, startIndex);
		for (int i = 0; i < path.length; i++) {
			TrieGramCounterHack counter = path[i];
			counter.updateCount(count);
			if (i > 0) {
				path[i - 1].updateMaps(counter, count);
			}
		}
	}

	private void updateMaps(TrieGramCounterHack successor, boolean added) {
		if (this.nature != 2) return;
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
			case 0: return getDistinctSuccessors1();
			case 1: return getDistinctSuccessors2();
			case 2: return getDistinctSuccessors3();
		}
		return 0;
	}

	private int getDistinctSuccessors1() {
		if (this.successor1 == null) return 0;
		else if (this.successor2 == null) return 1;
		else if (this.successor3 == null) return 2;
		else return 3;
	}

	private int getDistinctSuccessors2() {
		for (int i = 0; i < this.successorArray.length; i++) {
			if (this.successorArray[i] == null) return i;
		}
		return this.successorArray.length;
	}

	private int getDistinctSuccessors3() {
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
		TrieGramCounterHack[] counters = getPath(temp.stream(), startIndex);
		TrieGramCounterHack counter = counters[counters.length - 1];
		TrieGramCounterHack context = counters[counters.length - 2];
		counts[0] = counter == null ? 0 : counter.getCount();
		counts[1] = context.getCount();
		return counts;
	}

	@Override
	public int[] getDistinctCounts(int range, Stream<Integer> indices, int startIndex) {
		int[] distinctCounts = new int[range];
		TrieGramCounterHack[] counters = getPath(indices, startIndex);
		TrieGramCounterHack counter = counters[counters.length - 1];
		int overallCount = counter.countsMap.size();
		for (int i = 1; i < range; i++) {
			int countI = counter.countsMap.get(i).size();
			distinctCounts[i] = countI;
			overallCount -= countI;
		}
		distinctCounts[range - 1] = overallCount;
		return distinctCounts;
	}

	private TrieGramCounterHack[] getOrCreatePath(Stream<Integer> indices, int startIndex) {
		List<TrieGramCounterHack> path = new ArrayList<TrieGramCounterHack>();
		indices.skip(startIndex)
			.forEachOrdered(x -> extendPath(x, path, true));
		return path.toArray(new TrieGramCounterHack[path.size()]);
	}

	private TrieGramCounterHack[] getPath(Stream<Integer> indices, int startIndex) {
		List<TrieGramCounterHack> path = new ArrayList<TrieGramCounterHack>();
		indices.skip(startIndex)
			.forEachOrdered(x -> extendPath(x, path, false));
		return path.toArray(new TrieGramCounterHack[path.size()]);
	}

	private void extendPath(Integer x, List<TrieGramCounterHack> path, boolean grow) {
		if (path.isEmpty()) path.add(this);
		TrieGramCounterHack prev = path.get(path.size() - 1);
		if (prev == null) return;
		TrieGramCounterHack next;
		if (grow) next = prev.getOrPutSuccessor(x);
		else next = prev.getSuccessor(x);
		path.add(next);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.index);
		out.writeInt(this.count);
		switch (this.nature) {
			case 0: loadSingles(); break;
			case 1: loadArray(); break;
			default: break;
		}
		out.writeInt(this.successors.size());
		for (Entry<Integer, TrieGramCounterHack> entry : this.successors.entrySet()) {
			out.writeInt(entry.getKey());
			entry.getValue().writeExternal(out);
		}
	}

	private void loadArray() {
		initTreeStructure();
		Arrays.stream(this.successorArray)
			.forEachOrdered(x -> this.successors.put(x.getIndex(), x));
	}

	private void loadSingles() {
		if (this.successor1 == null) return;
		initTreeStructure();
		this.successors.put(this.successor1.getIndex(), this.successor1);
		if (this.successor2 != null) {
			this.successors.put(this.successor2.getIndex(), this.successor2);
			if (this.successor3 != null) {
				this.successors.put(this.successor3.getIndex(), this.successor3);
			}
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
		if (size <= 3) {
			this.successor1 = (TrieGramCounterHack) in.readObject();
			this.successor2 = (TrieGramCounterHack) in.readObject();
			this.successor3 = (TrieGramCounterHack) in.readObject();
		}
		else if (size <= ARRAY_SIZE) {
			this.successorArray = new TrieGramCounterHack[ARRAY_SIZE];
			for (int i = 0; i < size; i++) {
				this.successorArray[i] = (TrieGramCounterHack) in.readObject();
			}
		}
		else {
			this.successors = new HashMap<Integer, TrieGramCounterHack>(size, 1.0f);
			for (int i = 0; i < size; i++) {
				int index = in.readInt();
				TrieGramCounterHack counter = new TrieGramCounterHack();
				counter.readExternal(in);
				this.successors.put(index, counter);
			}
		}
	}

	private void initCountsMap() {
		if (this.successors != null) {
			this.countsMap = HashMultimap.create();
			for (TrieGramCounterHack counter : this.successors.values()) {
				this.countsMap.put(counter.getCount(), counter);
			}
		}
	}

	/*
	 * Functionality for switching/updating the nature of this counter
	 */
	private void updateNature(int count) {
		if (count <= 3) {
			if (this.nature != 0) setNewNature(0);
		} else {
			if (count <= 0.75*ARRAY_SIZE) {
				if (this.nature != 1) setNewNature(1);
			}
			else {
				if (this.nature != 2) setNewNature(2);
			}
		}
	}

	private void setNewNature(int newNature) {
		switch (this.nature) {
			case 0: updateNature1(newNature); break;
			case 1: updateNature2(newNature); break;
			case 2: updateNature3(newNature); break;
		}
		this.nature = newNature;
	}

	private void updateNature1(int newNature) {
		if (newNature == 0) return;
		else if (newNature == 1) {
			this.successorArray = new TrieGramCounterHack[ARRAY_SIZE];
			if (this.successor1 == null) return;
			this.successorArray[0] = this.successor1;
			if (this.successor2 == null) return;
			this.successorArray[1] = this.successor2;
			if (this.successor3 == null) return;
			this.successorArray[2] = this.successor3;
		}
		else {
			initTreeStructure();
			if (this.successor1 == null) return;
			this.successors.put(this.successor1.getIndex(), this.successor1);
			if (this.successor2 != null) {
				this.successors.put(this.successor2.getIndex(), this.successor2);
				if (this.successor3 != null) {
					this.successors.put(this.successor3.getIndex(), this.successor3);
				}
			}
		}
	}

	private void updateNature2(int newNature) {
		if (newNature == 1) return;
		else if (newNature == 0) {
			this.successorArray = null;
		}
		else {
			initTreeStructure();
			Arrays.stream(this.successorArray)
				.forEachOrdered(x -> this.successors.put(x.getIndex(), x));
			this.successorArray = null;
		}
	}

	private void updateNature3(int newNature) {
		if (newNature == 2) return;
		else if (newNature == 0) {
			this.successors = null;
			this.countsMap = null;
		}
		else {
			this.successorArray = new TrieGramCounterHack[ARRAY_SIZE];
			int i = 0;
			for (TrieGramCounterHack x : this.successors.values()) {
				this.successorArray[i++] = x;
			}
			this.successors = null;
			this.countsMap = null;
		}
	}

	private void initTreeStructure() {
		if (this.successors == null) {
			this.successors = new HashMap<Integer, TrieGramCounterHack>();
			this.countsMap = HashMultimap.create();	
		}
	}
}
