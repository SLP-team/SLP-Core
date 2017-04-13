package slp.core.counting.trie;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.List;

import slp.core.counting.Counter;

public class RevTrieCounter implements Counter {

	private TrieCounter counter;
	public RevTrieCounter() {
		this.counter = new TrieCounter(128);
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		this.counter.writeExternal(out);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.counter.readExternal(in);
	}

	@Override
	public int getCount() {
		return this.counter.getCount();
	}

	@Override
	public int[] getCounts(List<Integer> indices) {
		Collections.reverse(indices);
		int[] counts = this.counter.getCounts(indices);
		Collections.reverse(indices);
		return counts;
	}

	@Override
	public int getCountofCount(int n, int count) {
		return this.counter.getCountofCount(n, count);
	}

	@Override
	public int getSuccessorCount() {
		return this.counter.getSuccessorCount();
	}

	@Override
	public int getSuccessorCount(List<Integer> indices) {
		Collections.reverse(indices);
		int successorCount = this.counter.getSuccessorCount(indices);
		Collections.reverse(indices);
		return successorCount;
	}

	@Override
	public List<Integer> getTopSuccessors(List<Integer> indices, int limit) {
		Collections.reverse(indices);
		List<Integer> successors = this.counter.getTopSuccessors(indices, limit);
		Collections.reverse(indices);
		return successors;
	}

	@Override
	public int[] getDistinctCounts(int range, List<Integer> indices) {
		Collections.reverse(indices);
		int[] counts = this.counter.getDistinctCounts(range, indices);
		Collections.reverse(indices);
		return counts;
	}

	@Override
	public void addAggressive(List<Integer> indices) {
		Collections.reverse(indices);
		this.counter.addAggressive(indices);
		Collections.reverse(indices);
	}

	@Override
	public void removeAggressive(List<Integer> indices) {
		Collections.reverse(indices);
		this.counter.removeAggressive(indices);
		Collections.reverse(indices);
	}

	@Override
	public void addConservative(List<Integer> indices) {
		Collections.reverse(indices);
		this.counter.addConservative(indices);
		Collections.reverse(indices);
	}

	@Override
	public void removeConservative(List<Integer> indices) {
		Collections.reverse(indices);
		this.counter.removeConservative(indices);
		Collections.reverse(indices);
	}
	
}
