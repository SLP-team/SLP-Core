package slp.core.lexing.util;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;

public class ReverseLexer implements Lexer {
	
	private Lexer lexer;
	
	public ReverseLexer(Lexer lexer) {
		this.lexer = lexer;
	}

	public Stream<Stream<String>> lex(File file) {
		Stream<Stream<String>> lexed = this.lexer.lex(file);
		return reverse(lexed);
	}

	public Stream<Stream<String>> lex(String text) {
		Stream<Stream<String>> lexed = this.lexer.lex(text);
		return reverse(lexed);
	}

	public Stream<Stream<String>> lex(Stream<String> lines) {
		Stream<Stream<String>> lexed = this.lexer.lex(lines);
		return reverse(lexed);
	}
	
	@Override
	public Stream<Stream<String>> lex(List<String> lines) {
		Stream<Stream<String>> lexed = this.lexer.lex(lines);
		return reverse(lexed);
	}

	private Stream<Stream<String>> reverse(Stream<Stream<String>> lexed) {
		List<List<String>> reversed = lexed.map(l -> l.collect(Collectors.toList()))
				.peek(Collections::reverse)
				.collect(Collectors.toList());
		Collections.reverse(reversed);
		return reversed.stream().map(List::stream);
	}
}
