package slp.core.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class Writer {

	public static void writeContent(File file, String content) throws IOException {
		try (BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			fw.append(content);
		}
	}
	
	public static void writeTokens(File file, Stream<String> tokens) throws IOException {
		try (BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			tokens.forEachOrdered(x -> {
				try {
					fw.append(x);
					fw.append('\n');
				} catch (IOException e) {
					System.err.println(e);
				}
			});
		}
	}
	
	public static void writeLines(File file, Stream<String> lines) throws IOException {
		try (BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
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
		try (BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
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
