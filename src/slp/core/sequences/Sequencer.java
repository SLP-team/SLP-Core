package slp.core.sequences;

import java.util.stream.Stream;

import slp.core.tokenizing.Token;

public interface Sequencer {
	
	public Stream<Stream<Token>> sequence(Stream<Token> in);
	
	public Stream<Token> sequence(Token in);
	
	public static Sequencer standard() {
		return new NGramSequencer();
	}
}
