package slp.core.lexing.simple;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;

public class PunctuationLexer implements Lexer {

	@Override
	public Stream<Stream<String>> lex(Stream<String> lines) {
		return lines.map(line ->
					Arrays.stream(line.split("\\s+"))
						.flatMap(splitCarefully()));
	}
	
	@Override
	public Stream<Stream<String>> lex(List<String> lines) {
		return lines.stream().map(line ->
					Arrays.stream(line.split("\\s+"))
						.flatMap(splitCarefully()));
	}
	
	/**
	 * Splits on punctuation but leaves start/end-of-line and unk delimiters intact
	 * @return
	 */
	private Function<? super String, ? extends Stream<? extends String>> splitCarefully() {
		return x -> x.toLowerCase().matches("<((/)?s|unk)>")
					? Stream.of(x)
					: Arrays.stream(x.split("((?<=\\p{Punct})|(?=\\p{Punct}))"));
	}
}
