package slp.core.sequences;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import slp.core.counting.Vocabulary;
import slp.core.tokenizing.TokenizingRunner;

public class SequenceRunner {
	private final TokenizingRunner tokenizingRunner;
	private final SequenceMapper sequenceMapper;

	public SequenceRunner() {
		this(new TokenizingRunner());
	}

	public SequenceRunner(TokenizingRunner tokenizingRunner) {
		this(tokenizingRunner, new NGramMapper());
	}

	public SequenceRunner(SequenceMapper mapper) {
		this(new TokenizingRunner(), mapper);
	}

	public SequenceRunner(TokenizingRunner tokenizingRunner, SequenceMapper mapper) {
		this.tokenizingRunner = tokenizingRunner;
		this.sequenceMapper = new NGramMapper();
	}

	public TokenizingRunner getTokenizer() {
		return this.tokenizingRunner;
	}

	public SequenceMapper getSequenceMapper() {
		return this.sequenceMapper;
	}

	public Vocabulary getVocabulary() {
		return this.sequenceMapper.getVocabulary();
	}
	
	public Stream<Stream<Integer[]>> map(File file) throws IOException {
		return this.tokenizingRunner.map(file)
				.map(x -> x.map(this.sequenceMapper::map));
	}

	public Stream<Integer[]> flatMap(File file) throws IOException {
		return this.tokenizingRunner.flatMap(file)
				.map(this.sequenceMapper::map);
	}
}
