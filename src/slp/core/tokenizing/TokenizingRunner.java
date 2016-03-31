package slp.core.tokenizing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

public class TokenizingRunner  {

	private final Tokenizer tokenizer;

	public TokenizingRunner() {
		this(new SimpleTokenizer());
	}
	
	public TokenizingRunner(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}
	
	public Tokenizer getTokenizer() {
		return this.tokenizer;
	}
	
	public Stream<Stream<Token>> map(File file) throws IOException {
		return Files.lines(file.toPath())
			.map(this.tokenizer::tokenize);
	}

	public Stream<Token> flatMap(File file) throws IOException {
		return Files.lines(file.toPath())
			.flatMap(this.tokenizer::tokenize);
	}
}
