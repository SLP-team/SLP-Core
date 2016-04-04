package slp.core.tokenizing.io;

import slp.core.tokenizing.Token;

public class SimpleTokenizedReader extends TokenizedReader {

	@Override
	protected Token tokenFromString(String x) {
		return new Token(x);
	}
	
}
