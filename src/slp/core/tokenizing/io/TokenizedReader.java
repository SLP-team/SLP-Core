package slp.core.tokenizing.io;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import slp.core.tokenizing.Token;
import slp.core.util.Reader;

public abstract class TokenizedReader {
	public Stream<Stream<Token>> read(File file) {
		return Reader.readLines(file).map(x ->
					Arrays.stream(x.split("\\s+"))
						.map(y -> tokenFromString(y)));
	}

	protected abstract Token tokenFromString(String x);
}
