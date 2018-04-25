package slp.core.lexing.simple;

import java.util.Arrays;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;

public class WhitespaceLexer implements Lexer {

	@Override
	public Stream<String> lexLine(String line) {
		return Arrays.stream(line.split("\\s+"));
	}
	
}
