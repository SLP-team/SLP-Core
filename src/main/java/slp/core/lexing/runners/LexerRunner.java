package slp.core.lexing.runners;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import slp.core.io.Writer;
import slp.core.lexing.Lexer;
import slp.core.modeling.Model;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;

/**
 * This class can be used to run a {@linkplain Lexer} over bodies of code.
 * It differentiates between lexing each line separately or each file as a whole,
 * and adds several options, such as adding markers at the start and end of every sentence,
 * and only lexing files that match some extension/regular expression.
 * It also provides some util methods like {@link #lexDirectory(File)} and variants.
 * 
 * @author Vincent Hellendoorn
 */
public class LexerRunner {
	
	private final Lexer lexer;
	private final boolean perLine;

	private boolean sentenceMarkers = false;
	private String regex = ".*";

	/**
	 * Create a LexerRunner that wraps a {@link Lexer} and adds line separation if needed.
	 * <br />
	 * In some tasks (especially in NLP), a file with unrelated individual sentences on each line tends to be used,
	 * whereas in most code applications, we tend to use a complete code file in which the lines should be treated as a continuous block.
	 * The LexerRunner (and ModelRunner, which uses this class) need to know this to allow appropriate training.
	 * 
	 * @param lexer A {@link Lexer} that can produce a stream of tokens for each line in a File, or for single-line inputs.
	 * @param lexLinesSeparately Whether the data that this LexerRunner will consider is logically grouped by lines or files.
	 * 
	 */
	public LexerRunner(Lexer lexer, boolean lexLinesSeparately) {
		this.lexer = lexer;
		this.perLine = lexLinesSeparately;
	}
	
	/**
	 * Returns the lexer currently used by this class
	 */
	public Lexer getLexer() {
		return lexer;
	}

	/**
	 * Returns whether lexing adds delimiters per line.
	 */
	public boolean isPerLine() {
		return this.perLine;
	}

	/**
	 * Convenience method that adds sentence markers if those aren't yet present in the data.
	 * A {@link Model} always uses the first token as a ground truth (and thus does not model it)
	 * and models up to and including the last token.
	 * <br />
	 * If set to 'true', this adds delimiters (i.e. "&lt;s&gt;" and "&lt;/s&gt;"; see {@link Vocabulary}) to each sentence.
	 * A sentence is either every line in a file (if this LexerRunner is created to lex lines separately) or a whole file.
	 * <br />
	 * @param useDelimiters Whether to add delimiters to each sentence. Default: false, which assumes these have already been added.
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
	 * Lex each file in this directory to a their tokens grouped by lines, subject to the underlying {@link Lexer}
	 * and whether this Lexer is configured to work per line
	 * @param directory
	 * @return
	 */
	public Stream<Pair<File, Stream<Stream<String>>>> lexDirectory(File directory) {
		try {
			return Files.walk(directory.toPath())
				.map(Path::toFile)
				.filter(File::isFile)
				.filter(this::willLexFile)
				.map(fIn -> Pair.of(fIn, lexFile(fIn)));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Lex the provided file to a stream of tokens per line. Note that this is preferred over lex(lines),
	 * since knowing the file location/context can be helpful for most lexers!
	 * <br />
	 * <em>Note:</em> returns empty stream if the file does not match this builder's regex
	 * (which accepts everything unless set otherwise in {@link #setRegex(String)}).
	 * @param file File to lex
	 */
	public Stream<Stream<String>> lexFile(File file) {
		if (!willLexFile(file)) return Stream.empty();
		return lexTokens(this.lexer.lexFile(file));
	}

	/**
	 * Lex the provided text to a stream of tokens per line.
	 * <b>Note:</b> if possible, use lex(File) instead! Knowing the file location/context can benefit e.g. AST lexers.
	 *
	 * @param content Textual content to lex
	 */
	public Stream<Stream<String>> lexText(String content) {
		return lexTokens(this.lexer.lexText(content));
	}
	
	public Stream<String> lexLine(String line) {
		Stream<String> lexed = this.lexer.lexLine(line);
		if (this.sentenceMarkers) {
			lexed = Stream.concat(Stream.of(Vocabulary.BOS),
						Stream.concat(lexed, Stream.of(Vocabulary.EOS)));
		}
		return lexed;
	}
	
	/**
	 * Lex a directory recursively, provided for convenience.
	 * Creates a mirror-structure in 'to' that has the lexed (and translated if {@link #preTranslate()} is set) file for each input file
	 * @param from Source file/directory to be lexed
	 * @param to Target file/directory to be created with lexed (optionally translated) content from source
	 */
	public void lexDirectory(File from, File to) {
		this.lexDirectoryToIndices(from, to, null);
	}
	
	/**
	 * Lex a directory recursively, provided for convenience.
	 * Creates a mirror-structure in 'to' that has the lexed (and translated if {@link #preTranslate()} is set) file for each input file
	 * @param from Source file/directory to be lexed
	 * @param to Target file/directory to be created with lexed (optionally translated) content from source
	 * @param vocabulary The Vocabulary to translate the words to indices in said Vocabulary.
	 * 					 If no translation is required, use {@link #lexDirectory(File, File)}
	 */
	public void lexDirectoryToIndices(File from, File to, Vocabulary vocabulary) {
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
						Stream<Stream<String>> lexed = lexFile(fIn);
						lexed.map(l -> l.map(w -> vocabulary == null ? w : vocabulary.store(w)+""));
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

	private Stream<Stream<String>> lexTokens(Stream<Stream<String>> tokens) {
		if (this.sentenceMarkers) return withDelimiters(tokens);
		else return tokens;
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
