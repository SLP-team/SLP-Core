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
	private BiMap<Token, Integer> wordIndices;
	
	public static int size;
	
	public Vocabulary() {
		this.wordIndices = HashBiMap.create(20000);
	}
	
	public static Vocabulary create() {
		return new Vocabulary();
	}

	public Stream<Integer> translate(Stream<Token> tokens) {
		return tokens.map(this::translate);
	}
	
	public Integer translate(Token token) {
		return this.wordIndices.get(token);
	}
	
	public Stream<Integer> toIndices(Stream<Token> tokens) {
		return tokens.map(this::toIndex);
	}
	
	public Integer toIndex(Token token) {
		Integer index = this.wordIndices.get(token);
		if (index == null) {
			index = this.wordIndices.size();
			this.wordIndices.put(token, index);
			size = this.wordIndices.size();
		}
		return index;
	}

	public Stream<Token> findWords(Stream<Integer> indices) {
		return indices.map(this::findWord);
	}
	
	public Token findWord(Integer index) {
		return this.wordIndices.inverse().get(index);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.wordIndices);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.wordIndices = (BiMap<Token, Integer>) in.readObject();
	}
}
