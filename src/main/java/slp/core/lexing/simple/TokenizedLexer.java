package slp.core.lexing.simple;

import java.util.Arrays;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;

public class TokenizedLexer implements Lexer {

	@Override
	public Stream<String> lexLine(String line) {
		return Arrays.stream(line.split("\t")).filter(t -> !t.isEmpty());
	}
	
}
