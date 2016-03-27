package slp.tokenizing.core;

import java.util.stream.Stream;

public interface Tokenizer {

	public Stream<Token> tokenize(String text);
	
	public Stream<Token> tokenize(Stream<String> words);
	
}
