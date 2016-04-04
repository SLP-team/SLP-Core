package slp.core.counting;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import slp.core.tokenizing.Token;

public class Vocabulary implements Externalizable {

	private static final long serialVersionUID = -1572227007765465883L;
	private BiMap<String, Integer> wordIndices;
	
	public Vocabulary() {
		this.wordIndices = HashBiMap.create();
	}
	
	public static Vocabulary create() {
		return new Vocabulary();
	}

	public Stream<Integer> wordsToIndices(Stream<String> words) {
		return words.map(this::toIndex);
	}
	
	public Integer toIndex(String word) {
		Integer index = this.wordIndices.get(word);
		if (index == null) {
			index = this.wordIndices.size();
			this.wordIndices.put(word, index);
		}
		return index;
	}

	public Stream<Integer> toIndices(Stream<Token> tokens) {
		return tokens.map(this::toIndex);
	}
	
	public Integer toIndex(Token token) {
		return toIndex(token.text());
	}
	
	public Stream<Integer> findWordIndices(Stream<String> words) {
		return words.map(this::findIndex);
	}
	
	public Integer findIndex(String word) {
		return this.wordIndices.get(word);
	}
	
	public Stream<Integer> findIndices(Stream<Token> tokens) {
		return tokens.map(this::findIndex);
	}
	
	public Integer findIndex(Token token) {
		return findIndex(token.text());
	}
	
	public Stream<String> findWords(Stream<Integer> indices) {
		return indices.map(this::findWord);
	}
	
	public String findWord(Integer index) {
		return this.wordIndices.inverse().get(index);
	}
	
	public BiMap<String, Integer> getWordIndices() {
		return wordIndices;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.wordIndices);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.wordIndices = (BiMap<String, Integer>) in.readObject();
	}
}
