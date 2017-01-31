package slp.core.counting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import slp.core.io.Reader;
import slp.core.tokenizing.Token;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Configuration;

public class Vocabulary {

	private static final Token UNK = new Token("<UNK>");
	private Map<Token, Integer> wordIndices;
	private List<Token> words;
	private List<Integer> counts;
	
	private boolean closed;
	
	public Vocabulary() {
		this(16);
	}

	public Vocabulary(int expectedSize) {
		this.wordIndices = new HashMap<>();
		this.words = new ArrayList<>();
		this.counts = new ArrayList<>();
		addUnk();
	}
	
	private Vocabulary(boolean addUnk) {
		this(16, addUnk);
	}

	private Vocabulary(int expectedSize, boolean addUnk) {
		this.wordIndices = new HashMap<>();
		this.words = new ArrayList<>();
		this.counts = new ArrayList<>();
		if (addUnk) addUnk();
	}

	private void addUnk() {
		this.wordIndices.put(UNK, 0);
		this.words.add(UNK);
		this.counts.add(0);
	}
	
	public int size() {
		return this.words.size();
	}
	
	private int checkPoint;
	public void setCheckpoint() {
		this.checkPoint = this.words.size();
		this.open();
	}
	
	public void restoreCheckpoint() {
		for (int i = this.words.size(); i > this.checkPoint; i--) {
			this.counts.remove(this.counts.size() - 1);
			Token word = this.words.remove(this.words.size() - 1);
			this.wordIndices.remove(word);
		}
		this.close();
	}
	
	public void close() {
		this.closed = true;
	}

	public void open() {
		this.closed = false;
	}
	
	public Stream<Integer> translate(Stream<Token> tokens) {
		return tokens.map(this::translate);
	}
	
	public Integer translate(String text) {
		return this.wordIndices.get(text);
	}
	
	public Integer translate(Token token) {
		return this.wordIndices.get(token);
	}
	
	public Stream<Integer> toIndices(Stream<Token> tokens) {
		return tokens.map(this::toIndex);
	}
	
	public synchronized Integer toIndex(Token token) {
		Integer index = this.wordIndices.get(token);
		if (index == null) {
			if (this.closed) {
				return this.wordIndices.get(UNK);
			}
			else {
				index = this.wordIndices.size();
				this.wordIndices.put(token, index);
				this.words.add(token);
				this.counts.add(1);
			}
		}
		return index;
	}

	public Stream<Token> findWords(Stream<Integer> indices) {
		return indices.map(this::toWord);
	}
	
	public Token toWord(Integer index) {
		return this.words.get(index);
	}
	
	private int insert(Token token) {
		Integer index = this.wordIndices.get(token);
		if (index == null) {
			index = this.counts.size();
			this.wordIndices.put(token, index);
			this.words.add(token);
			this.counts.add(0);
		}
		this.counts.set(index, this.counts.get(index) + 1);
		return index;
	}

	public static Vocabulary empty() {
		return new Vocabulary();
	}

	public static Vocabulary fromFile(File file) throws NumberFormatException {
		return fromFile(file, 0);
	}
	
	public static Vocabulary fromFile(File file, int cutoff) throws NumberFormatException {
		Vocabulary vocabulary = new Vocabulary(false);
		Reader.readLines(file)
			.map(x -> x.split("\t"))
			.filter(x -> cutoff <= 0 || Integer.parseInt(x[0]) >= cutoff)
			.forEachOrdered(split -> {
				Integer count = Integer.parseInt(split[0]);
				Token key = new Token(split[1]);
				Integer index = Integer.parseInt(split[2]);
				if (index != vocabulary.words.size()) {
					System.err.println("Index misalignment for " + Arrays.toString(split)
						+ " (vocab size: " + vocabulary.words.size() + ")");
				}
				vocabulary.counts.add(count);
				vocabulary.words.add(key);
				vocabulary.wordIndices.put(key, index);
			});
		// A vocabulary must contain UNK
		if (!vocabulary.wordIndices.containsKey(UNK)) {
			vocabulary.wordIndices.put(UNK, vocabulary.wordIndices.size());
		}
		vocabulary.close();
		return vocabulary;
	}
	
	public static void toFile(Vocabulary vocabulary, File file) throws IOException {
		try (Writer fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			for (int i = 0; i < vocabulary.size(); i++) {
				Integer count = vocabulary.counts.get(i);
				Token word = vocabulary.words.get(i);
				Integer index = vocabulary.wordIndices.get(word);
				fw.append(count + "\t" + word.toString() + "\t" + index + "\n");
			}
		}
	}

	public static Vocabulary build(Tokenizer tokenizer, File... files) throws NumberFormatException, IOException {
		return build(tokenizer, Configuration.unkCutof(), files);
	}

	public static Vocabulary build(Tokenizer tokenizer, List<File> files) throws NumberFormatException, IOException {
		return build(tokenizer, Configuration.unkCutof(), files.toArray(new File[0]));
	}
	
	public static Vocabulary build(Tokenizer tokenizer, int countCutoff, File... files) throws NumberFormatException, IOException {
		Vocabulary temp = new Vocabulary();
		int c = 0;
		for (File file : files) {
			if (++c % 1000 == 0) System.out.println("Building @ " + c + " of " + files.length);
			Stream.of(Reader.readContent(file))
				.flatMap(tokenizer::tokenize)
				.sequential()
				.forEach(temp::insert);
		}
		// Normalize vocabulary
		List<Integer> collect = IntStream.range(1, temp.size()).mapToObj(i -> i).sorted(new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				Integer c1 = temp.counts.get(o1);
				Integer c2 = temp.counts.get(o2);
				return -10*c1.compareTo(c2) + o1.compareTo(o2);
			}
		}).collect(Collectors.toList());
		Vocabulary vocabulary = new Vocabulary();
		int unkCount = 0;
		for (int i : collect) {
			Token token = temp.words.get(i);
			int count = temp.counts.get(i);
			if (count < countCutoff) {
				unkCount += count;
			}
			else {
				vocabulary.wordIndices.put(token, vocabulary.wordIndices.size());
				vocabulary.words.add(token);
				vocabulary.counts.add(count);
			}
		}
		vocabulary.counts.set(0, vocabulary.counts.get(0) + unkCount);
		// Start at 1 so as to not remove UNK token
		vocabulary.close();
		return vocabulary;
	}

	public static void writeVocabulary(Vocabulary vocabulary, File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		ObjectOutputStream o = new ObjectOutputStream(out);
		o.writeObject(vocabulary);
		o.close();
	}
	
	public static Vocabulary readVocabulary(File file) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		Vocabulary vocabulary = (Vocabulary) ois.readObject();
		ois.close();
		return vocabulary;
	}
}
