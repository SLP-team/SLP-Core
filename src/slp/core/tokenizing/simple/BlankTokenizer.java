package slp.core.tokenizing.simple;

import java.util.Arrays;
import java.util.stream.Stream;

import slp.core.tokenizing.Token;
import slp.core.tokenizing.Tokenizer;

public class BlankTokenizer implements Tokenizer {

	@Override
	public Stream<Token> tokenize(String text) {
		return Arrays.stream(text.split("\\s+")).map(Token::new);
	}
	
	@Override
	public Token tokenizeWord(String word) {
		return new Token(word);
	}
}
