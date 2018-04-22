package slp.core.lexing.util;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;

public class ReverseLexer implements Lexer {
	
	private final Lexer lexer;
	
	public ReverseLexer(Lexer lexer) {
		this.lexer = lexer;
	}

	@Override
	public Stream<Stream<String>> lexFile(File file) {
		Stream<Stream<String>> lexed = this.lexer.lexFile(file);
		return reverse(lexed);
	}

	@Override
	public Stream<Stream<String>> lexText(String text) {
		Stream<Stream<String>> lexed = this.lexer.lexText(text);
		return reverse(lexed);
	}

	@Override
	public Stream<String> lexLine(String line) {
		Stream<String> lexed = this.lexer.lexLine(line);
		List<String> collect = lexed.collect(Collectors.toList());
		Collections.reverse(collect);
		return collect.stream();
	}

	private Stream<Stream<String>> reverse(Stream<Stream<String>> lexed) {
		List<List<String>> reversed = lexed.map(l -> l.collect(Collectors.toList()))
				.peek(Collections::reverse)
				.collect(Collectors.toList());
		Collections.reverse(reversed);
		return reversed.stream().map(List::stream);
	}
}
