package slp.core.tokenizing.io;

import slp.core.tokenizing.Token;

public class SimpleTokenizedWriter extends TokenizedWriter {

	@Override
	protected String tokenToString(Token token) {
		return token.text();
	}
	
}
