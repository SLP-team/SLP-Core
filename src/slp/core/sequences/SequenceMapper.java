package slp.core.sequences;

import java.util.stream.Stream;

import slp.core.counting.Vocabulary;
import slp.core.tokenizing.Token;

public interface SequenceMapper {
	
	public Stream<Integer[]> map(Stream<Token> in);
	
	public Integer[] map(Token in);
	
	public Vocabulary getVocabulary();
}
