package slp.core.lexing.runners;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import slp.core.io.Writer;
import slp.core.lexing.Lexer;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.translating.Vocabulary;

/**
 * The this is the starting point of any modeling code,
 * since any input should be lexed first and many models need access to lexing even at test time.
 * This class can be configured statically and exposes lexing methods that are used by each model.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class LexerRunner {
	
	private final Lexer lexer;
	private final Vocabulary vocabulary;
	
	private boolean perLine = false;
	private boolean sentenceMarkers = false;
	private String regex = ".*";
	private boolean translate = false;

	public LexerRunner() {
		this(new PunctuationLexer());
	}

	public LexerRunner(Lexer lexer) {
		this(lexer, new Vocabulary());
	}

	public LexerRunner(Lexer lexer, Vocabulary vocabulary) {
		this.lexer = lexer;
		this.vocabulary = vocabulary;
	}
	
	public Vocabulary getVocabulary() {
		return this.vocabulary;
	}
	
	/**
	 * Returns the lexer currently used by this class
	 */
	public Lexer getLexer() {
		return lexer;
	}

	/**
	 * If set to 'true', enforces adding delimiters to the text to lex
	 * (i.e. "&lt;s&gt;", ""&lt;/s&gt;"; see {@link Vocabulary}), to each sentence.
	 * A sentence is a whole file by default, unless {@link #setPerLine(boolean)} is set to true
	 * in which case each line is treated as a sentence).
	 * <br />
	 * Default: false, which assumes these have already been added.
	 * @return
	 */
	public void setSentenceMarkers(boolean useDelimiters) {
		this.sentenceMarkers = useDelimiters;
	}
	
	/**
	 * Returns whether or not file/line (depending on {@code perLine}) sentence markers are added.
	 */
	public boolean hasSentenceMarkers() {
		return this.sentenceMarkers;
	}
	
	/**
	 * If set to 'true', enforces lexing each line separately.
	 * This only has effect is {@link #useDelimiters()} is set,
	 * in which case this method prepends delimiters on each line rather than the full content.
	 * <br />
	 * Default: set to false.
	 */
	public void setPerLine(boolean perLine) {
		this.perLine = perLine;
	}
	
	/**
	 * Returns whether lexing adds delimiters per line.
	 */
	public boolean isPerLine() {
		return this.perLine;
	}

	/**
	 * Specify regex for file extensions to be kept.
	 * <br />
	 * <em>Note:</em> to just specify the extension, use the more convenient {@link #setExtension(String)}.
	 * @param regex Regular expression to match file name against. E.g. ".*\\.(c|h)" for C source and header files.
	 */
	public void setRegex(String regex) {
		this.regex = regex;
	}
	
	/**
	 * Alternative to {@link #setRegex(String)} that allows you to specify just the extension.
	 * <br />
	 * <em>Note:</em> this prepends <code>.*\\.</code> to the provided regex!
	 * @param regex Regular expression to match against extension of files. E.g. "(c|h)" for C source and header files.
	 */
	public void setExtension(String regex) {
		this.regex = ".*\\." + regex;
	}
	
	/**
	 * Returns the regex currently used to filter input files to lex.
	 */
	public String getRegex() {
		return this.regex;
	}

	/**
	 * Returns whether the file matches the regex and will thus be lexed by this class
	 */
	public boolean willLexFile(File file) {
		return file.getName().matches(this.regex);
	}
	
	/**
	 * Convenience method that translates tokens to indices after lexing before writing to file (default: no translation).
	 * <br />
	 * <em>Note:</em> you should either initialize the vocabulary yourself or write it to file afterwards
	 * (as {@link slp.core.CLI} does) or the resulting indices are (mostly) meaningless.
	 */
	public void setPreTranslate(boolean preTranslate) {
		this.translate = preTranslate;
	}
	
	/**
	 * Lex a directory recursively, provided for convenience.
	 * Creates a mirror-structure in 'to' that has the lexed (and translated if {@link #preTranslate()} is set) file for each input file
	 * @param from Source file/directory to be lexed
	 * @param to Target file/directory to be created with lexed (optionally translated) content from source
	 */
	public void lexDirectory(File from, File to) {
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
	 * Lex the provided file to a stream of tokens per line. Note that this is preferred over lex(lines),
	 * since knowing the file location/context can be for most lexers!
	 * <br />
	 * <em>Note:</em> returns empty stream if the file does not match this builder's regex
	 * (which accepts everything unless set otherwise in {@link #setRegex(String)}).
	 * @param file File to lex
	 */
	public Stream<Stream<String>> lex(File file) {
		if (!willLexFile(file)) return Stream.empty();
		return lexTokens(this.lexer.lex(file));
	}

	/**
	 * Lex the provided text to a stream of tokens per line.
	 * <b>Note:</b> if possible, use lex(File) instead! Knowing the file location/context can benefit e.g. AST lexers.
	 *
	 * @param content Textual content to lex
	 */
	public Stream<Stream<String>> lex(String content) {
		return lexTokens(this.lexer.lex(content));
	}
	
	/**
	 * Lex the provided lines (see {@link slp.core.io.Reader}) to a stream of tokens per line, possibly adding delimiters.
	 * <b>Note:</b> if possible, use lex(File) instead! Knowing the file location/context can benefit e.g. AST lexers.
	 * 
	 * @param lines Lines to lex
	 * @return A Stream of lines containing a Stream of tokens each
	 */
	public Stream<Stream<String>> lex(Stream<String> lines) {
		return lexTokens(this.lexer.lex(lines));
	}

	private Stream<Stream<String>> lexTokens(Stream<Stream<String>> tokens) {
		Stream<Stream<String>> lexed = tokens
				.map(this.vocabulary::toIndices)
				.map(l -> l.map(t -> this.translate ? t+"" : this.vocabulary.toWord(t)));
		if (this.sentenceMarkers) return withDelimiters(lexed);
		else return lexed;
	}

	private Stream<Stream<String>> withDelimiters(Stream<Stream<String>> lexed) {
		if (this.perLine) {
			return lexed.map(l -> Stream.concat(Stream.of(Vocabulary.BOS), Stream.concat(l, Stream.of(Vocabulary.EOS))));
		}
		else {
			// Concatenate the BOS token with the first sub-stream (first line's stream) specifically to avoid off-setting all the lines.
			// The EOS token is just appended as an extra line
			int[] c = { 0 };
			lexed = lexed.map(l -> c[0]++ == 0 ? Stream.concat(Stream.of(Vocabulary.BOS), l) : l);
			return Stream.concat(lexed, Stream.of(Stream.of(Vocabulary.EOS)));
		}
	}
}
