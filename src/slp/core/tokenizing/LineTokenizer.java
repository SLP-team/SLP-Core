package slp.core.tokenizing;

import java.util.Arrays;
import java.util.stream.Stream;

public class LineTokenizer implements Tokenizer {

	@Override
	public Stream<Token> tokenize(String text) {
		return Arrays.stream(text.split("\n")).map(Token::new);
	}
	
	@Override
	public Token tokenizeWord(String word) {
		return new Token(word);
	}
}
