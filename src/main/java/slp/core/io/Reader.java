package slp.core.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Reader {

	public static Stream<String> readLines(File file) {
		try {
			CharsetDecoder dec = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.IGNORE);
			try (BufferedReader br = new BufferedReader(Channels.newReader(FileChannel.open(file.toPath()), dec, -1))) {
				List<String> lines = br.lines().collect(Collectors.toList());
				return lines.stream();
			}
		} catch (IOException | UncheckedIOException e) {
			System.err.println("Reader.readLines(): Files.lines failed, reading full file using BufferedReader instead");
			List<String> lines = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					lines.add(line);
				}
			} catch (IOException e2) {
				e2.printStackTrace();
				return null;
			}
			return lines.stream();
		}
	}
}
