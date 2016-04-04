package slp.core.tokenizing;

import java.util.stream.Stream;

public interface Tokenizer {

	public Stream<Token> tokenize(String text);
	
	public Stream<Token> tokenize(Stream<String> words);

	public Token tokenizeWord(String word);
	
	public static Tokenizer standard() {
		return new SimpleTokenizer();
	}
}
