package slp.core.tokenizing;

import java.util.stream.Stream;

public interface Tokenizer {

	public Stream<Token> tokenize(String text);
	
	public default Stream<Token> tokenize(Stream<String> words) {
		return words.map(this::tokenizeWord);
	}

	public default Token tokenizeWord(String word) {
		return new Token(word);
	}
	
	public static Tokenizer standard() {
		return new SimpleTokenizer();
	}
}
