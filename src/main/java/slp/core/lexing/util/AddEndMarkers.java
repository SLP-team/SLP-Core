package slp.core.lexing.util;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;
import slp.core.translating.Vocabulary;

public class AddEndMarkers implements Lexer {
	
	private final Lexer lexer;
	
	public AddEndMarkers(Lexer lexer) {
		this.lexer = lexer;
	}

	@Override
	public Stream<Stream<String>> lexFile(File file) {
		Stream<Stream<String>> lexed = this.lexer.lexFile(file);
		return lexed.map(this::addEOL);
	}

	@Override
	public Stream<Stream<String>> lexText(String text) {
		Stream<Stream<String>> lexed = this.lexer.lexText(text);
		return lexed.map(this::addEOL);
	}

	@Override
	public Stream<String> lexLine(String line) {
		Stream<String> lexed = this.lexer.lexLine(line);
		return addEOL(lexed);
	}

	private Stream<String> addEOL(Stream<String> lexed) {
		List<String> line = lexed.collect(Collectors.toList());
		return line.isEmpty() ? line.stream() : Stream.concat(line.stream(), Stream.of(Vocabulary.EOS));
	}
}
