package slp.core.tokenizing.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import slp.core.tokenizing.Token;
import slp.core.tokenizing.Tokenizer;

public class JavaTokenizer implements Tokenizer {

	public Stream<Token> tokenize(String text) {
		IScanner scanner = ToolFactory.createScanner(false, false, true, "1.4", "1.5");
		return process(text, scanner).stream().map(Token::new);
	}

	public List<List<String>> tokenizeLines(String text) {
		IScanner scanner = ToolFactory.createScanner(false, false, true, "1.4", "1.5");
		text = filterUnderScores(text);
		text = filterBinLits(text);
		scanner.setSource(text.toCharArray());
		List<List<String>> lineTokens = new ArrayList<>();
		List<String> tokens = new ArrayList<>();
		tokens.add("<s>");
		lineTokens.add(new ArrayList<>());
		lineTokens.get(0).add("<s>");
		int nextToken = 0;
		int line = 0;
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
			if (val.length() >= 15) {
				if (val.startsWith("\"")) val = "\"\"";
			}
			if (val.startsWith("\"") && val.endsWith("\"")) {
				val = val.replaceAll("\n", "\\n");
				val = val.replaceAll("\r", "\\r");
				val = val.replaceAll("\t", "\\t");
			}
			else if (val.startsWith("\'") && val.endsWith("\'")) {
				val = val.replaceAll("\n", "\\n");
				val = val.replaceAll("\r", "\\r");
				val = val.replaceAll("\t", "\\t");
			}
			// For Java, we have to add heuristic check regarding breaking up >>
			if (val.matches(">>+")) {
				boolean split = false;
				for (int i = tokens.size() - 1; i >= 0; i--) {
					String token = tokens.get(i);
					if (token.matches("[,\\.\\?\\[\\]]") || Character.isUpperCase(token.charAt(0))
							|| token.equals("extends") || token.equals("super")) {
						continue;
					}
					else if (token.equals("<")) {
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
		tokens.add("</s>");
		lineTokens.get(lineTokens.size() - 1).add("</s>");
		return lineTokens;
	}

	private List<String> process(String text, IScanner scanner) {
		text = filterUnderScores(text);
		text = filterBinLits(text);
		scanner.setSource(text.toCharArray());
		List<String> tokens = new ArrayList<>();
		tokens.add("<s>");
		int nextToken = 0;
		while (true) {
			try {
				nextToken = scanner.getNextToken();
			} catch (InvalidInputException e) {
				continue;
			}
			if (nextToken == ITerminalSymbols.TokenNameEOF) break;
			String val = new String(scanner.getCurrentTokenSource());
			if (val.length() >= 15) {
				if (val.startsWith("\"")) val = "\"\"";
			}
			if (val.startsWith("\"") && val.endsWith("\"")) {
				val = val.replaceAll("\n", "\\n");
				val = val.replaceAll("\r", "\\r");
				val = val.replaceAll("\t", "\\t");
			}
			else if (val.startsWith("\'") && val.endsWith("\'")) {
				val = val.replaceAll("\n", "\\n");
				val = val.replaceAll("\r", "\\r");
				val = val.replaceAll("\t", "\\t");
			}
			// For Java, we have to add heuristic check regarding breaking up >>
			if (val.matches(">>+")) {
				boolean split = false;
				for (int i = tokens.size() - 1; i >= 0; i--) {
					String token = tokens.get(i);
					if (token.matches("[,\\.\\?\\[\\]]") || Character.isUpperCase(token.charAt(0))
							|| token.equals("extends") || token.equals("super")) {
						continue;
					}
					else if (token.equals("<")) {
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
					}
					continue;
				}
			}
			tokens.add(val);
		}
		tokens.add("</s>");
		return tokens;
	}

	@SuppressWarnings("unused")
	private List<String> splitID(String test) {
		List<String> parts = new ArrayList<>();
		StringBuilder curr = new StringBuilder();
		char[] chars = test.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c == '_') {
				if (curr.length() > 0) {
					parts.add(curr.toString());
					curr = new StringBuilder();
				}
				continue;
			}
			if (Character.isUpperCase(c)) {
				if (curr.length() > 0 && !Character.isUpperCase(curr.charAt(curr.length() - 1))) {
					parts.add(curr.toString());
					curr = new StringBuilder();
				}
				curr.append(c);
			}
			else {
				if (curr.length() > 1 && Character.isUpperCase(curr.charAt(curr.length() - 1))
						 && Character.isUpperCase(curr.charAt(curr.length() - 2))) {
					parts.add(curr.substring(0, curr.length() - 1));
					char last = curr.charAt(curr.length() - 1);
					curr = new StringBuilder();
					curr.append(last);
				}
				curr.append(c);
			}
		}
		if (curr.length() > 0) parts.add(curr.toString());
		return parts;
	}
	
	private String filterUnderScores(String text) {
		// Have to first filter out underscores in numerals as tokenizer doesn't support SE 7 standard
		Pattern p = Pattern.compile("[0-9]_[0-9]");
		Matcher m = p.matcher(text);
		StringBuilder newText = new StringBuilder();
		int prev = 0;
		while (m.find()) {
			newText.append(text.substring(prev, m.start() + 1));
			prev = m.end() - 1;
		}
		newText.append(text.substring(prev, text.length()));
		return newText.toString();
	}

	private String filterBinLits(String text) {
		// Have to first filter out underscores in numerals as tokenizer doesn't support SE 7 standard
		Pattern p = Pattern.compile("0b[01]");
		Matcher m = p.matcher(text);
		StringBuilder newText = new StringBuilder();
		int prev = 0;
		while (m.find()) {
			newText.append(text.substring(prev, m.start() + 1));
			prev = m.end() - 1;
		}
		newText.append(text.substring(prev, text.length()));
		return newText.toString();
	}
	
	public boolean isIdentifier(String token) {
		return !isKeyword(token) && token.matches(ID_REGEX);
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
		return JavaTokenizer.KEYWORD_SET.contains(token);
	}

	public static final String[] KEYWORDS = { "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
			"class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally",
			"float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
			"new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
			"switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
			"true", "false", "null" };

	public static final Set<String> KEYWORD_SET = new HashSet<String>(Arrays.asList(KEYWORDS));
}
