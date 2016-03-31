package slp.core.counting;

public class FastTrieGramCounter extends TrieGramCounter {
	
	public FastTrieGramCounter() {
		super();
	}
	
	public FastTrieGramCounter(Integer index) {
		super(index);
	}

	@Override
	public void update(Integer[] indices, boolean count, int startIndex, int endIndex) {
		if (startIndex > endIndex) {
			System.err.println("Invalid start/end index " + startIndex + "/" + endIndex);
			return;
		}
		if (count) inc(); else dec();
		if (startIndex < endIndex) {
			Integer atI = indices[startIndex];
			FastTrieGramCounter successor = getOrPutSuccessor(atI);
			this.getCounts().get(successor.getCount()).remove(successor);
			successor.update(indices, count, startIndex + 1, endIndex);
			if (successor.getCount() > 0) {
				this.getCounts().get(successor.getCount()).add(successor);
			}
		}
	}

	private FastTrieGramCounter getOrPutSuccessor(Integer key) {
		FastTrieGramCounter trieGramCounter = (FastTrieGramCounter) getSuccessor(key);
		if (trieGramCounter == null) {
			trieGramCounter = new FastTrieGramCounter(key);
			getSuccessors().put(key, trieGramCounter);
		}
		return trieGramCounter;
	}
}
