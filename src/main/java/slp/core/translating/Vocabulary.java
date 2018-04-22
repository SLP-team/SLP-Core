package slp.core.translating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Translation (to integers) is the second step (after Lexing) before any modeling takes place.
 * The this is global (static) and is open by default; it can be initialized through
 * the {@link thisRunner} class or simply left open to be filled by the modeling code
 * (as has been shown to be more appropriate for modeling source code).
 * <br />
 * <em>Note:</em> the counts in this class are for informative purposes only:
 * these are not (to be) used by any model nor updated with training.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class Vocabulary {

	public static final String UNK = "<UNK>";
	public static final String BOS = "<s>";
	public static final String EOS = "</s>";
	
	private Map<String, Integer> wordIndices;
	private List<String> words;
	private List<Integer> counts;
	
	public Vocabulary() {
		this.wordIndices = new HashMap<>();
		this.words = new ArrayList<>();
		this.counts = new ArrayList<>();
		this.closed = false;
		addUnk();	
	}
	
	private boolean closed;

	private void addUnk() {
		this.wordIndices.put(UNK, 0);
		this.words.add(UNK);
		this.counts.add(0);
	}
	
	public Map<String, Integer> wordIndices() {
		return wordIndices;
	}
	
	public List<String> getWords() {
		return words;
	}
	
	public List<Integer> getCounts() {
		return counts;
	}
	
	public int size() {
		return this.words.size();
	}
	
	public void close() {
		closed = true;
	}

	public void open() {
		closed = false;
	}
	
	private int checkPoint;
	public void setCheckpoint() {
		checkPoint = words.size();
	}
	
	public void restoreCheckpoint() {
		for (int i = words.size(); i > checkPoint; i--) {
			counts.remove(counts.size() - 1);
			String word = words.remove(words.size() - 1);
			wordIndices.remove(word);
		}
	}
	
	public int store(String token) {
		return store(token, 1);
	}
		
	public int store(String token, int count) {
		Integer index = wordIndices.get(token);
		if (index == null) {
			index = wordIndices.size();
			wordIndices.put(token, index);
			words.add(token);
			counts.add(count);
		}
		else {
			counts.set(index, count);
		}
		return index;
	}
	
	public Stream<Integer> toIndices(Stream<String> tokens) {
		return tokens.map(this::toIndex);
	}

	public List<Integer> toIndices(List<String> tokens) {
		return tokens.stream().map(this::toIndex).collect(Collectors.toList());
	}

	public Integer toIndex(String token) {
		Integer index = wordIndices.get(token);
		if (index == null) {
			if (closed) {
				return wordIndices.get(UNK);
			}
			else {
				index = wordIndices.size();
				wordIndices.put(token, index);
				words.add(token);
				counts.add(1);
			}
		}
		return index;
	}
	
	public Integer getCount(String token) {
		Integer index = wordIndices.get(token);
		if (index != null) {
			return getCount(index);
		}
		return 0;
	}

	private Integer getCount(Integer index) {
		return counts.get(index);
	}

	public Stream<String> toWords(Stream<Integer> indices) {
		return indices.map(this::toWord);
	}

	public List<String> toWords(List<Integer> indices) {
		return indices.stream().map(this::toWord).collect(Collectors.toList());
	}
		
	public String toWord(Integer index) {
		return words.get(index);
	}
}
