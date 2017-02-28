package slp.core.tokenizing.simple;

import java.util.Arrays;
import java.util.stream.Stream;

import slp.core.tokenizing.Token;
import slp.core.tokenizing.Tokenizer;

public class PreTokenizer implements Tokenizer {

	@Override
	public Stream<Token> tokenize(String text) {
		return Arrays.stream(text.split("\n")).map(this::tokenizeWord);
	}
	
	@Override
	public Token tokenizeWord(String word) {
		return new Token(word);
	}
}
