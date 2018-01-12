package slp.core.lexing.util;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;
import slp.core.lexing.LexerRunner;

public class ReverseLexer implements Lexer {
	
	private Lexer lexer;

	public ReverseLexer() {
		this(LexerRunner.getLexer());
	}
	
	public ReverseLexer(Lexer lexer) {
		this.lexer = lexer;
	}

	public Stream<Stream<String>> lex(File file) {
		return reverse(this.lexer.lex(file));
	}

	public Stream<Stream<String>> lex(String text) {
		return reverse(this.lexer.lex(text));
	}

	public Stream<Stream<String>> lex(Stream<String> lines) {
		return reverse(this.lexer.lex(lines));
	}
	
	@Override
	public Stream<Stream<String>> lex(List<String> lines) {
		return reverse(this.lexer.lex(lines));
	}

	private Stream<Stream<String>> reverse(Stream<Stream<String>> lexed) {
		List<List<String>> reversed = lexed.map(l -> l.collect(Collectors.toList()))
			.peek(l -> Collections.reverse(l))
			.collect(Collectors.toList());
		Collections.reverse(reversed);
		return reversed.stream().map(List::stream);
	}
}
