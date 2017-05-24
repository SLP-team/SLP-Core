package slp.core.lexing.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import slp.core.lexing.Lexer;

public class JavaLexer implements Lexer {
	
	@Override
	public Stream<Stream<String>> lex(List<String> lines) {
		String text = lines.stream().collect(Collectors.joining("\n"));
		return tokenizeLines(text).stream().map(List::stream);
	}

	public List<List<String>> tokenizeLines(String text) {
		IScanner scanner = ToolFactory.createScanner(false, false, true, "1.8");
		scanner.setSource(text.toCharArray());
		List<List<String>> lineTokens = new ArrayList<>();
		List<String> tokens = new ArrayList<>();
		lineTokens.add(new ArrayList<>());
		int nextToken = 0;
		int line = 1;
		while (true) {
			try {
				nextToken = scanner.getNextToken();
				if (nextToken == ITerminalSymbols.TokenNameEOF) break;
				int ln = scanner.getLineNumber(scanner.getCurrentTokenStartPosition());
				if (ln > line) {
					for (int i = line + 1; i <= ln; i++) lineTokens.add(new ArrayList<>());
					line = ln;
				}
			} catch (InvalidInputException e) {
				continue;
			}
			String val = new String(scanner.getCurrentTokenSource());
			if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 2) {
				if (val.length() >= 15) {
					val = "\"\"";
				}
				else {
					String body = val.substring(1, val.length() - 1);
					body = body.replaceAll("\\\\", "\\\\\\\\");
					body = body.replaceAll("\"", "\\\\\"");
					body = body.replaceAll("\n", "\\n");
					body = body.replaceAll("\r", "\\r");
					body = body.replaceAll("\t", "\\t");
					val = "\"" + body + "\"";
				}
			}
			else if (val.startsWith("\'") && val.endsWith("\'")) {
				val = val.replaceAll("\n", "\\n");
				val = val.replaceAll("\r", "\\r");
				val = val.replaceAll("\t", "\\t");
			}
			// For Java, we have to add heuristic check regarding breaking up >>
			else if (val.matches(">>+")) {
				boolean split = false;
				for (int i = tokens.size() - 1; i >= 0; i--) {
					String token = tokens.get(i);
					if (token.matches("[,\\.\\?\\[\\]]") || Character.isUpperCase(token.charAt(0))
							|| token.equals("extends") || token.equals("super")
							|| token.matches("(byte|short|int|long|float|double)")) {
						continue;
					}
					else if (token.matches("(<|>)+")) {
						split = true;
						break;
					}
					else {
						break;
					}
				}
				if (split) {
					for (int i = 0; i < val.length(); i++) {
						tokens.add(">");
						lineTokens.get(lineTokens.size() - 1).add(">");
					}
					continue;
				}
			}
			tokens.add(val);
			lineTokens.get(lineTokens.size() - 1).add(val);
		}
		return lineTokens;
	}

	private static final String ID_REGEX = "[a-zA-Z_$][a-zA-Z\\d_$]*";
	private static final String HEX_REGEX = "0x([0-9a-fA-F]+_)*[0-9a-fA-F]+[lLfFdD]?";
	private static final String BIN_REGEX = "0b([01]+_)*[01]+[lL]";
	private static final String IR_REGEX = "([0-9]+_)*[0-9]+[lLfFdD]?";
	// A: nrs before and after dot, B: nrs only before dot, C nrs only after, D: only E as indicator
	private static final String DBL_REGEXA = "[0-9]+\\.[0-9]+([eE][-+]?[0-9]+)?[fFdD]?";
	private static final String DBL_REGEXB = "[0-9]+\\.([eE][-+]?[0-9]+)?[fFdD]?";
	private static final String DBL_REGEXC = "\\.[0-9]+([eE][-+]?[0-9]+)?[fFdD]?";
	private static final String DBL_REGEXD = "[0-9]+[eE][-+]?[0-9]+[fFdD]?";

	public static boolean isID(String token) {
		return !isKeyword(token) && token.matches(ID_REGEX);
	}

	public static boolean isNR(String token) {
		return token.matches("(" + HEX_REGEX + "|" + IR_REGEX + "|" + BIN_REGEX +
				"|" + DBL_REGEXA + "|" + DBL_REGEXB + "|" + DBL_REGEXC + "|" + DBL_REGEXD + ")");
	}

	public static boolean isSTR(String token) {
		return token.matches("\".+\"");
	}

	public static boolean isChar(String token) {
		return token.matches("'.+'");
	}

	public static boolean isKeyword(String token) {
		return JavaLexer.KEYWORD_SET.contains(token);
	}

	public static final String[] KEYWORDS = { "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
			"class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally",
			"float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
			"new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
			"switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
			"true", "false", "null" };

	public static final Set<String> KEYWORD_SET = new HashSet<String>(Arrays.asList(KEYWORDS));
}
