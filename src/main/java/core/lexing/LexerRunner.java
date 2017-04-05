package core.lexing;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import core.io.Reader;
import core.io.Writer;
import core.lexing.simple.PunctuationLexer;
import core.translating.Vocabulary;

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
	private static boolean delimiters = false;
	private static String regex = ".*";
	
	/**
	 * Specify lexer to be used for tokenizing. Default: {@link PunctuationLexer}.
	 * @param lexer The lexer to lex the input with
	 */
	public static void setLexer(Lexer lexer) {
		LexerRunner.lexer = lexer;
	}

	/**
	 * Enforce adding delimiters to text to lex (i.e. "&lt;SOL"&gt;", ""&lt;EOL"&gt;"; see {@link Vocabulary}) if not present.
	 * <br />
	 * Default: false, which assumes these have already been added.
	 * <br /> <em>Note:</em> to 
	 * <br /> <em>Note:</em> current implementation adds SOL/EOL markers with a single space,
	 * which might be incompatible with the tokenizer used. May be revised in the future.
	 * @return
	 */
	public static void useDelimiters(boolean useDelimiters) {
		LexerRunner.delimiters = useDelimiters;
	}
	
	/**
	 * Enforce lexing each line separately. This only has effect is {@link #useDelimiters()} is set,
	 * in which case this method prepends delimiters on each line rather than the full content.
	 */
	public static void perLine(boolean perLine) {
		LexerRunner.perLine = perLine;
	}

	/**
	 * Convenience method that translates tokens to indices after lexing before writing to file (default: no translation).
	 * <br />
	 * <em>Note:</em> you should either initialize the vocabulary yourself or write it to file afterwards
	 * (as {@link core.CLI} does) or the resulting indices are (mostly) meaningless.
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
	 * Lex the provided file to a stream of tokens per line.
	 * <br />
	 * <em>Note:</em> returns empty stream if the file does not match this builder's regex
	 * (which accepts everything unless set otherwise in {@link #useRegex(String)}).
	 * @param file File to lex
	 */
	public static List<List<String>> lex(File file) {
		if (file.getName().matches(regex)) return lex(Reader.readLines(file));
		else return Collections.emptyList();
	}
	
	/**
	 * Lex the provided lines (see {@link core.io.Reader}) to a stream of tokens per line, possibly adding delimiters
	 * @param lines Lines to lex
	 * @return A Stream of lines containing a Stream of tokens each
	 */
	public static List<List<String>> lex(List<String> lines) {
		List<List<String>> lexed = lexer.lex(lines)
				.map(l -> l.map(t -> translate ? ""+Vocabulary.toIndex(t) : t))
				.map(l -> l.collect(Collectors.toList()))
				.filter(l -> !l.isEmpty())
				.collect(Collectors.toList());
		if (lexed.isEmpty()) return Collections.emptyList();
		if (delimiters) {
			if (perLine) {
				lexed.stream().forEach(LexerRunner::padLine);
			}
			else {
				padStart(lexed.get(0));
				padEnd(lexed.get(lexed.size() - 1));
			}
		}
		return lexed;
	}
	
	private static void padLine(List<String> line) {
		padStart(line);
		padEnd(line);
	}

	private static void padStart(List<String> line) {
		if (translate) {
			if (!Vocabulary.toWord(Integer.parseInt(line.get(0))).equals(Vocabulary.SOL)) {
				line.add(0, ""+Vocabulary.toIndex(Vocabulary.SOL));
			}
		}
		else if (!translate) {
			if (!line.get(0).equals(Vocabulary.SOL)) {
				line.add(0, Vocabulary.SOL);
			}
		}
	}

	private static void padEnd(List<String> line) {
		String last = line.get(line.size() - 1);
		if (translate) {
			if (!Vocabulary.toWord(Integer.parseInt(last)).equals(Vocabulary.EOL)) {
				line.add(""+Vocabulary.toIndex(Vocabulary.EOL));
			}
		}
		else if (!translate) {
			if (!last.equals(Vocabulary.EOL)) {
				line.add(Vocabulary.EOL);
			}
		}
	}

	/**
	 * Lex a directory recursively, provided for convenience.
	 * Creates a mirror-structure in 'to' that has the lexed (and translated if {@link #preTranslate()} is set) file for each input file
	 * @param from Source file/directory to be lexed
	 * @param to Target file/directory to be created with lexed (optionally translated) content from source
	 */
	public static void lexDirectory(File from, File to) {
		if (from.isFile()) {
			List<List<String>> lexed = lex(from);
			if (lexed.isEmpty()) return;
			try {
				Writer.writeTokens(to, lexed);
			} catch (IOException e) {
				System.out.println("Exception in LexerBuilder.tokenize(), from " + from + " to " + to);
				e.printStackTrace();
			}
		}
		else {
			for (String child : from.list()) {
				File fIn = new File(from, child);
				File fOut = new File(to, child);
				if (fIn.isDirectory()) {
					fOut.mkdir();
				}
				lexDirectory(fIn, fOut);
			}
		}
	}
}
