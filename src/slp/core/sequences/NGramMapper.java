package slp.core.sequences;

import java.util.stream.Stream;

import slp.core.counting.Vocabulary;
import slp.core.tokenizing.Token;
import slp.core.util.Buffer;

public class NGramMapper implements SequenceMapper {

	private final Vocabulary vocabulary;
	private final Buffer<Integer> buffer;

	public NGramMapper() {
		this(new Vocabulary());
	}
	
	public NGramMapper(Vocabulary vocabulary) {
		this(vocabulary, Buffer.of(Integer.class));
	}
	
	public NGramMapper(Vocabulary vocabulary, Buffer<Integer> buffer) {
		this.vocabulary = vocabulary;
		this.buffer = buffer;
	}

	@Override
	public Vocabulary getVocabulary() {
		return vocabulary;
	}

	@Override
	public Stream<Integer[]> map(Stream<Token> in) {
		return in.map(this::map);
	}

	@Override
	public Integer[] map(Token in) {
		return this.buffer.apply(this.getVocabulary().toIndex(in));
	}
}
