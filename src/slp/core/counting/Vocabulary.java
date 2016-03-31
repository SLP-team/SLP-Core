package slp.core.counting;

import slp.core.tokenizing.Token;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class Vocabulary {
	private final BiMap<String, Integer> wordIndices;
	
	public Vocabulary() {
		this.wordIndices = HashBiMap.create();
	}
	
	public Integer toIndex(String word) {
		Integer index = this.wordIndices.get(word);
		if (index == null) {
			index = this.wordIndices.size();
			this.wordIndices.put(word, index);
		}
		return index;
	}
	
	public Integer toIndex(Token token) {
		return toIndex(token.text());
	}
	
	public Integer findIndex(String word) {
		return this.wordIndices.get(word);
	}
	
	public Integer findIndex(Token token) {
		return findIndex(token.text());
	}
	
	public String findWord(Integer index) {
		return this.wordIndices.inverse().get(index);
	}
	
	public BiMap<String, Integer> getWordIndices() {
		return wordIndices;
	}
}
