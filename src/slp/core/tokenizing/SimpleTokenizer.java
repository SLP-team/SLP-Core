package slp.core.tokenizing;

import java.util.Arrays;
import java.util.stream.Stream;

public class SimpleTokenizer implements Tokenizer {

	@Override
	public Stream<Token> tokenize(String text) {
		return Arrays.stream(text.split("\\s+")).map(this::tokenizeWord);
	}
	
	@Override
	public Token tokenizeWord(String word) {
		return new Token(word);
	}
}
