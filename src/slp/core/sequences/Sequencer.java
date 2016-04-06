package slp.core.sequences;

import java.util.stream.Stream;

import slp.core.tokenizing.Token;

public interface Sequencer {

	public default Stream<Stream<Token>> sequence(Stream<Token> in) {
		this.reset();
		return in.map(this::sequence);
	}
	
	public Stream<Token> sequence(Token in);
	
	public void reset();

	public static Sequencer standard() {
		return new NGramSequencer();
	}
}
