package slp.core.lexing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import slp.core.io.Reader;
import slp.core.io.Writer;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.translating.Vocabulary;

/**
 * The LexerRunner is the starting point of any modeling code,
 * since any input should be lexed first and many models need access to lexing even at test time.
 * This class can be configured statically and exposes static lexing methods that are used by each model.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class LexerRunner {
	
	private static Lexer lexer = new PunctuationLexer();
	private static boolean translate = false;
	private static boolean perLine = false;
	private static boolean sentenceMarkers = false;
	private static String regex = ".*";
	
	/**
	 * Specify lexer to be used for tokenizing. Default: {@link PunctuationLexer}.
	 * @param lexer The lexer to lex the input with
	 */
	public static void setLexer(Lexer lexer) {
		LexerRunner.lexer = lexer;
	}
	
	/**
	 * Returns the lexer currently used by this class
	 */
	public static Lexer getLexer() {
		return LexerRunner.lexer;
	}

	/**
	 * Enforce adding delimiters to text to lex (i.e. "&lt;s&gt;", ""&lt;/s&gt;"; see {@link Vocabulary})
	 * to each sentence (by default the whole file, unless {@link #perLine(boolean)} is set,
	 * in which case each line is treated as a sentence).
	 * <br />
	 * Default: false, which assumes these have already been added.
	 * @return
	 */
	public static void addSentenceMarkers(boolean useDelimiters) {
		LexerRunner.sentenceMarkers = useDelimiters;
	}
	
	/**
	 * Returns whether or not file/line (depending on {@code perLine}) sentence markers are added.
	 */
	public static boolean addsSentenceMarkers() {
		return LexerRunner.sentenceMarkers;
	}
	
	/**
	 * Enforce lexing each line separately. This only has effect is {@link #useDelimiters()} is set,
	 * in which case this method prepends delimiters on each line rather than the full content.
	 */
	public static void perLine(boolean perLine) {
		LexerRunner.perLine = perLine;
	}
	
	/**
	 * Returns whether lexing adds delimiters per line.
	 */
	public static boolean isPerLine() {
		return LexerRunner.perLine;
	}

	/**
	 * Convenience method that translates tokens to indices after lexing before writing to file (default: no translation).
	 * <br />
	 * <em>Note:</em> you should either initialize the vocabulary yourself or write it to file afterwards
	 * (as {@link slp.core.CLI} does) or the resulting indices are (mostly) meaningless.
	 */
	public static void preTranslate(boolean preTranslate) {
		LexerRunner.translate = preTranslate;
	}
	
	/**
	 * Specify regex for file extensions to be kept.
	 * <br />
	 * <em>Note:</em> to just specify the extension, use the more convenient {@link #useExtension(String)}.
	 * @param regex Regular expression to match file name against. E.g. ".*\\.(c|h)" for C source and header files.
	 */
	public static void useRegex(String regex) {
		LexerRunner.regex = regex;
	}
	
	/**
	 * Alternative to {@link #useRegex(String)} that allows you to specify just the extension.
	 * <br />
	 * <em>Note:</em> this prepends <code>.*\\.</code> to the provided regex!
	 * @param regex Regular expression to match against extension of files. E.g. "(c|h)" for C source and header files.
	 */
	public static void useExtension(String regex) {
		LexerRunner.regex = ".*\\." + regex;
	}
	
	/**
	 * Returns the regex currently used to filter input files to lex.
	 */
	public static String getRegex() {
		return LexerRunner.regex;
	}
	
	/**
	 * Lex a directory recursively, provided for convenience.
	 * Creates a mirror-structure in 'to' that has the lexed (and translated if {@link #preTranslate()} is set) file for each input file
	 * @param from Source file/directory to be lexed
	 * @param to Target file/directory to be created with lexed (optionally translated) content from source
	 */
	public static void lexDirectory(File from, File to) {
		int[] count = { 0 };
		try {
			Files.walk(from.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.forEach(fIn -> {
					if (++count[0] % 1000 == 0) {
						System.out.println("Lexing at file " + count[0]);
					}
					String path = to.getAbsolutePath() + fIn.getAbsolutePath().substring(from.getAbsolutePath().length());
					File fOut = new File(path);
					File outDir = fOut.getParentFile();
					outDir.mkdirs();
					try {
						Stream<Stream<String>> lexed = lex(fIn);
						Writer.writeTokenized(fOut, lexed);
					} catch (IOException e) {
						System.out.println("Exception in LexerBuilder.tokenize(), from " + fIn + " to " + fOut);
						e.printStackTrace();
					}
				});
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Lex the provided file to a stream of tokens per line.
	 * <br />
	 * <em>Note:</em> returns empty stream if the file does not match this builder's regex
	 * (which accepts everything unless set otherwise in {@link #useRegex(String)}).
	 * @param file File to lex
	 */
	public static Stream<Stream<String>> lex(File file) {
		if (file.getName().matches(regex)) return lex(Reader.readLines(file));
		else return Stream.empty();
	}
	
	/**
	 * Lex the provided lines (see {@link slp.core.io.Reader}) to a stream of tokens per line, possibly adding delimiters
	 * @param lines Lines to lex
	 * @return A Stream of lines containing a Stream of tokens each
	 */
	public static Stream<Stream<String>> lex(Stream<String> lines) {
		Stream<Stream<String>> lexed = lexer.lex(lines)
				.map(l -> l.map(Vocabulary::toIndex))
				.map(l -> l.map(t -> translate ? t+"" : Vocabulary.toWord(t)));
		if (sentenceMarkers) return withDelimiters(lexed);
		else return lexed;
	}

	private static Stream<Stream<String>> withDelimiters(Stream<Stream<String>> lexed) {
		if (perLine) {
			return lexed.map(l -> Stream.concat(Stream.of(Vocabulary.BOS), Stream.concat(l, Stream.of(Vocabulary.EOS))));
		}
		else {
			// Concatenate the BOS token with the first sub-stream (first line's stream) specifically to avoid off-setting all the lines.
			// The EOS token is just appended as an extra line
			int[] c = { 0 };
			lexed = lexed.map(l -> c[0]++ < 1 ? Stream.concat(Stream.of(Vocabulary.BOS), l) : l);
			return Stream.concat(lexed, Stream.of(Stream.of(Vocabulary.EOS)));
		}
	}
}
