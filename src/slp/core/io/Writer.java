package slp.core.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Stream;

public class Writer {
	
	public static void writeContent(File file, String content) throws IOException {
		try (FileWriter fw = new FileWriter(file)) {
			fw.append(content);
		}
	}
	
	public static void writeLines(File file, Stream<String> lines) throws IOException {
		try (FileWriter fw = new FileWriter(file)) {
			lines.forEachOrdered(x -> {
				try {
					fw.append(x);
					fw.append('\n');
				} catch (IOException e) {
					System.err.println(e);
				}
			});
		}
	}
	
	public static void writeWords(File file, Stream<Stream<String>> lines) throws IOException {
		try (FileWriter fw = new FileWriter(file)) {
			lines.forEachOrdered(x -> {
					x.forEachOrdered(y -> {
						try {
							fw.append(y);
							fw.append(' ');
						} catch (IOException e) {
							System.err.println(e);
						}
					});
					try {
						fw.append('\n');
					} catch (Exception e) {
						System.err.println(e);
					}
				}
			);
		}
	}
}
