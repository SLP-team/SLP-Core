package slp.counting.core;

import slp.tokenizing.core.Token;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class Vocabulary {
	private final BiMap<String, Integer> wordIndices;
	
	public Vocabulary() {
		this.wordIndices = HashBiMap.create();
	}

	public Integer getIndex(String word) {
		return this.wordIndices.get(word);
	}
	
	public Integer getIndex(Token word) {
		return this.wordIndices.get(word.text());
	}
	
	public String getWord(Integer index) {
		return this.wordIndices.inverse().get(index);
	}
	
	public BiMap<String, Integer> getWordIndices() {
		return wordIndices;
	}
}
