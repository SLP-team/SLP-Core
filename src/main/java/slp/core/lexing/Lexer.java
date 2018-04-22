package slp.core.lexing;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.io.Reader;

public interface Lexer {
	
	/**
	 * Lex all the lines in the provided file. Use of this method is preferred, since some Lexers benefit from knowing
	 * the file path (e.g. AST Lexers can use this for type inference).
	 * By default, invokes {@link #lexText(String)} with content of file.
	 * 
	 * @param file The file to be lexed
	 * @return A Stream of lines, where every line is lexed to a Stream of tokens
	 */
	default Stream<Stream<String>> lexFile(File file) {
		return lexText(Reader.readLines(file).stream().collect(Collectors.joining("\n")));
	}

	/**
	 * Lex the provided text. The default implementation invokes {@linkplain #lexLine(String)} on each line in the text,
	 * but sub-classes may opt to lex the text as a whole instead (e.g. JavaLexer needs to do so to handle comments correctly).
	 * 
	 * @param file The text to be lexed
	 * @return A Stream of lines, where every line is lexed to a Stream of tokens
	 */

	default Stream<Stream<String>> lexText(String text) {
		return Arrays.stream(text.split("\n")).map(this::lexLine);
	}

	/**
	 * Lex the provided line into a stream of tokens.
	 * The default implementations of {@link #lexFile(File)} and {@link #lexText(String)} refer to this method,
	 * but sub-classes may override that behavior to take more advantage of the full content.
	 * 
	 * @param line The line to be lexed
	 * @return A Stream of tokens that are present on this line (may be an empty Stream).
	 */
	Stream<String> lexLine(String line);
}
