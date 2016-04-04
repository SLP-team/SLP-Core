package slp.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Reader {

	public static String readContent(File file) {
		try {
			return Files.lines(file.toPath(), StandardCharsets.ISO_8859_1)
					.collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Stream<String> readLines(File file) {
		try {
			return Files.lines(file.toPath(), StandardCharsets.ISO_8859_1);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Stream<Stream<String>> readWords(File file) {
		try {
			return Files.lines(file.toPath(), StandardCharsets.ISO_8859_1)
					.map(x -> Arrays.stream(x.split("\\s+")));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
