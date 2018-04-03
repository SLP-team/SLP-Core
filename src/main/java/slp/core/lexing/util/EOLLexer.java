package slp.core.lexing.util;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;
import slp.core.translating.Vocabulary;

public class EOLLexer implements Lexer {
	
	private Lexer lexer;
	
	public EOLLexer(Lexer lexer) {
		this.lexer = lexer;
	}

	public Stream<Stream<String>> lex(File file) {
		Stream<Stream<String>> lexed = this.lexer.lex(file);
		return addEOL(lexed);
	}

	public Stream<Stream<String>> lex(String text) {
		Stream<Stream<String>> lexed = this.lexer.lex(text);
		return addEOL(lexed);
	}

	public Stream<Stream<String>> lex(Stream<String> lines) {
		Stream<Stream<String>> lexed = this.lexer.lex(lines);
		return addEOL(lexed);
	}
	
	@Override
	public Stream<Stream<String>> lex(List<String> lines) {
		Stream<Stream<String>> lexed = this.lexer.lex(lines);
		return addEOL(lexed);
	}

	private Stream<Stream<String>> addEOL(Stream<Stream<String>> lexed) {
		return lexed.map(l -> l.collect(Collectors.toList()))
				.map(l -> l.isEmpty() ? l.stream() : Stream.concat(l.stream(), Stream.of(Vocabulary.EOS)));
	}
}
