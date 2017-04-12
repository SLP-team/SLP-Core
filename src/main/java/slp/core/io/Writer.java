package core.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Writer {

	public static void writeContent(File file, String content) throws IOException {
		try (BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			fw.append(content);
		}
	}
	
	public static void writeTokens(File file, List<List<String>> lines) throws IOException {
		try (BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			for (int i = 0; i < lines.size(); i++) {
				List<String> line = lines.get(i);
				for (int j = 0; j < line.size(); j++) {
					String token = line.get(j);
					fw.append(token.replaceAll("\n", "\\n").replaceAll("\t", "\\t"));
					if (j < line.size() - 1) fw.append('\t');
				}
				if (i < lines.size() - 1) fw.append('\n');
			}
		}
	}
}
