package slp.core.tokenizing;

import java.util.Arrays;
import java.util.stream.Stream;

public class SimpleTokenizer implements Tokenizer {

	@Override
	public Stream<Token> tokenize(String text) {
		Stream<Token> start = text.startsWith("<s>") ? Stream.empty() : Stream.of(new Token("<s>"));
		Stream<Token> line = Arrays.stream(text.split("\\s+"))
				.flatMap(x -> Arrays.stream(x.split("((?<=\\p{Punct})|(?=\\p{Punct}))")))
				.map(this::tokenizeWord);
		Stream<Token> end = text.endsWith("</s>") ? Stream.empty() : Stream.of(new Token("</s>"));
		return Stream.concat(start, Stream.concat(line, end));
	}
	
	@Override
	public Token tokenizeWord(String word) {
		return new Token(word);
	}
}
