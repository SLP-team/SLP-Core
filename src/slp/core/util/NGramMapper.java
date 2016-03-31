package slp.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import slp.core.counting.Vocabulary;
import slp.core.tokenizing.SimpleTokenizer;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Buffer;
import slp.core.util.Configuration;

public class NGramMapper  {

	private final Tokenizer tokenizer;
	private final Vocabulary vocabulary;

	public NGramMapper() {
		this(new SimpleTokenizer());
	}
	
	public NGramMapper(Tokenizer tokenizer) {
		this(tokenizer, new Vocabulary());
	}
	
	public NGramMapper(Vocabulary vocabulary) {
		this(new SimpleTokenizer(), vocabulary);
	}

	public NGramMapper(Tokenizer tokenizer, Vocabulary vocabulary) {
		this.tokenizer = tokenizer;
		this.vocabulary = vocabulary;
	}

	public Tokenizer getTokenizer() {
		return this.tokenizer;
	}
	
	public Vocabulary getVocabulary() {
		return this.vocabulary;
	}
	
	public Stream<Integer[]> map(File file) throws IOException {
		Buffer<Integer> buffer = new Buffer<Integer>(Configuration.order());
		return Files.lines(file.toPath())
			.flatMap(this.tokenizer::tokenize)
			.map(this.vocabulary::toIndex)
			.map(buffer::apply);
	}
}
