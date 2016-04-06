package slp.core.sequences;

import java.util.stream.Stream;

import slp.core.tokenizing.Token;
import slp.core.util.Buffer;

public class NGramSequencer implements Sequencer {

	private final Buffer<Token> buffer;

	public NGramSequencer() {
		this.buffer = new Buffer<Token>();
	}

	@Override
	public Stream<Token> sequence(Token in) {
		return this.buffer.apply(in);
	}

	@Override
	public void reset() {
		this.buffer.clean();
	}
}
