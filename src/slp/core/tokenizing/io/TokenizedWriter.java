package slp.core.tokenizing.io;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import slp.core.io.Writer;
import slp.core.tokenizing.Token;

public abstract class TokenizedWriter {
	public void writeTokens(File file, Stream<Stream<Token>> tokens) throws IOException {
		Writer.writeWords(file, tokens.map(x -> x.map(this::tokenToString)));
	}
	
	protected abstract String tokenToString(Token token);
}
