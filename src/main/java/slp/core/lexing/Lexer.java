package core.lexing;

import java.util.List;
import java.util.stream.Stream;

public interface Lexer {
	Stream<Stream<String>> lex(List<String> lines);
}
