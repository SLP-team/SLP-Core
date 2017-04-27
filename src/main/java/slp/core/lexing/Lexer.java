package slp.core.lexing;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Lexer {
	default Stream<Stream<String>> lex(Stream<String> lines) {
		return lex(lines.collect(Collectors.toList()));
	}
	Stream<Stream<String>> lex(List<String> lines);
}
