package slp.core.tokenizing.java;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.tokenizing.Token;
import slp.core.tokenizing.Tokenizer;

public class SimpleJavaTokenizer implements Tokenizer {

	@Override
	public Stream<Token> tokenize(String text) {
		text = removeLineComments(text);
		text = removeBlockComments(text);
		Map<String, String> replacements = new HashMap<String, String>();
		text = removeStrings(text, replacements);
		
		// Split on punctuation and whitespace, but first establish what operators not to break
		String[] delimeters = new String[] { ">>>=", ">>>", "<<<=", "<<<", ">>=", ">>", "<<=", "<<", ">=", "<=",
				"++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "^=", "!=", "&&", "||", "==", "//", "/**", "/*", "*/" };
		String delimiterString = Arrays.stream(delimeters).map(Pattern::quote).collect(Collectors.joining("|"));
		String delimiterRegex = "((?<=(" + delimiterString + "))|(?=(" + delimiterString + ")))";
		String punctuation = "((?<=\\p{Punct})|(?=\\p{Punct}))";
		return Arrays.stream(text.split(delimiterRegex))
			.map(x -> x.matches(delimiterString) ? new String[]{ x } : x.split(punctuation))
			.flatMap(x -> Arrays.stream(x))
			.map(x -> replacements.containsKey(x) ? replacements.get(x) : x)
			.map(Token::new);
	}

	private String removeStrings(String text, Map<String, String> replacements) {
		Pattern stringFilter = Pattern.compile("\"[^\"]*\"");
		Matcher m = stringFilter.matcher(text);
		String rep = text;
		int idx = 0;
		while (m.find()) {
			String placeholder = "STRPLCHLDR" + idx++;
			replacements.put(placeholder, m.group());
			rep = rep.replace(m.group(), placeholder);
		}
		text = rep;
		return text;
	}

	private String removeBlockComments(String text) {
		String temp = text;
		String start = Pattern.quote("/**") + "?";
		String end = Pattern.quote("*/");
		Pattern blockComments = Pattern.compile(start + "[^(" + end + ")]*" + end, Pattern.DOTALL);
		Matcher m = blockComments.matcher(text);
		while (m.find()) {
			temp = temp.replace(m.group(), "");
		}
		text = temp;
		return text;
	}

	private String removeLineComments(String text) {
		String temp = text;
		Pattern lineComments = Pattern.compile("//[^\n]*\n");
		Matcher m = lineComments.matcher(text);
		while (m.find()) {
			temp = temp.replace(m.group(), "");
		}
		text = temp;
		return text;
	}

}
