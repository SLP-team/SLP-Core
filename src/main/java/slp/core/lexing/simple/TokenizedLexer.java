package core.lexing.simple;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import core.lexing.Lexer;

public class TokenizedLexer implements Lexer {

	@Override
	public Stream<Stream<String>> lex(List<String> lines) {
		return lines.stream().map(line -> Arrays.stream(line.split("\t")));
	}
}
