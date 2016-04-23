package slp.core.counting;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import slp.core.tokenizing.Token;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Reader;

public class Vocabulary implements Externalizable {

	private static final Token UNK = new Token("<UNK>");
	private static final long serialVersionUID = -1572227007765465883L;
	private BiMap<Token, Integer> wordIndices;
	
	public static int size;
	private boolean closed;
	
	public Vocabulary() {
		this.wordIndices = HashBiMap.create(20000);
		this.wordIndices.put(UNK, 0);
	}
	
	public void close() {
		this.closed = true;
	}
	
	public static Vocabulary empty() {
		return new Vocabulary();
	}

	public static Vocabulary fromFile(File file) throws NumberFormatException, IOException {
		Vocabulary vocabulary = new Vocabulary();
		vocabulary.wordIndices.remove(UNK);
		Files.lines(file.toPath(), StandardCharsets.ISO_8859_1)
			.forEachOrdered(x -> {
				String[] split = x.split("\\s+");
				Token key = new Token(split[0]);
				Integer value = Integer.parseInt(split[1]);
				vocabulary.wordIndices.put(key, value);
			});
		// A vocabulary must contain UNK
		if (!vocabulary.wordIndices.containsKey(UNK)) {
			vocabulary.wordIndices.put(UNK, vocabulary.wordIndices.size());
		}
		Vocabulary.size = vocabulary.wordIndices.size();
		vocabulary.close();
		return vocabulary;
	}
	
	public static Vocabulary build(Tokenizer tokenizer, int countCutoff, File... files) throws NumberFormatException, IOException {
		Vocabulary vocabulary = new Vocabulary();
		List<Integer> wordCounts = new ArrayList<Integer>();
		// UNK count
		wordCounts.add(0);
		for (File file : files) {
			Reader.readLines(file)
				.map(tokenizer::tokenize)
				.forEach(x -> x.forEach(token -> {
					Integer index = vocabulary.wordIndices.get(token);
					if (index == null) {
						index = vocabulary.wordIndices.size();
						vocabulary.wordIndices.put(token, index);
						wordCounts.add(0);
					}
					wordCounts.set(index, wordCounts.get(index) + 1);
				}));
		}
		BiMap<Integer, Token> inverse = vocabulary.wordIndices.inverse();
		// Do not remove UNK token
		for (int i = 1; i < wordCounts.size(); i++) {
			if (wordCounts.get(i) < countCutoff) {
				inverse.remove(i);
			}
		}
		Vocabulary.size = vocabulary.wordIndices.size();
		vocabulary.close();
		return vocabulary;
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
			if (this.closed) {
				return this.wordIndices.get(UNK);
			}
			else {
				index = this.wordIndices.size();
				this.wordIndices.put(token, index);
			}
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
		out.writeInt(this.wordIndices.size());
		for (Entry<Token, Integer> entry : this.wordIndices.entrySet()) {
			out.writeObject(entry.getKey());
			out.writeInt(entry.getValue());
		}
		out.writeObject(this.wordIndices);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int size = in.readInt();
		this.wordIndices = HashBiMap.create(size);
		for (int i = 0; i < size; i++) {
			wordIndices.put((Token) in.readObject(), in.readInt());
		}
	}
}
