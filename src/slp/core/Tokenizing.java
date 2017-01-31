package slp.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import slp.core.counting.Vocabulary;
import slp.core.io.Reader;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Util;

public class Tokenizing {

	static void tokenize(Tokenizer tokenizer, File inDir, File outDir, boolean flatten) {
		tokenize(tokenizer, inDir, outDir, null, flatten);
	}
	
	static void tokenize(Tokenizer tokenizer, File inDir, File outDir, Vocabulary vocabulary, boolean flatten) {
		try {
			if (flatten) Tokenizing.tokenizeFlat(tokenizer, inDir, outDir, vocabulary);
			else Tokenizing.tokenize(tokenizer, inDir, outDir, vocabulary);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void tokenizeFlat(Tokenizer tokenizer, File inDir, File outDir, Vocabulary vocabulary) throws FileNotFoundException, IOException {
		List<File> files = Util.getFiles(inDir);
		for (int i = 0; i < files.size(); i++) {
			File inFile = files.get(i);
			File outFile = new File(outDir, ""+i);
			Stream<String> tokens = Stream.of(Reader.readContent(inFile))
				.flatMap(tokenizer::tokenize)
				.map(t -> t.text().replaceAll("\n", "\\n"))
				.map(t -> vocabulary == null ? t : ""+vocabulary.translate(t));
			slp.core.io.Writer.writeTokens(outFile, tokens);
		}
	}

	private static void tokenize(Tokenizer tokenizer, File inDir, File outDir, Vocabulary vocabulary) throws FileNotFoundException, IOException {
		for (String child : inDir.list()) {
			File f = new File(inDir, child);
			if (f.isDirectory()) {
				File out = new File(outDir, f.getName());
				out.mkdir();
				tokenize(tokenizer, f, out, vocabulary);
			}
			else if (f.isFile()) {
				File outFile = new File(outDir, f.getName());
				Stream<String> tokens = Stream.of(Reader.readContent(f))
					.flatMap(tokenizer::tokenize)
					.map(t -> t.text().replaceAll("\n", "\\n"))
					.map(t -> vocabulary == null ? t : ""+vocabulary.translate(t));
				slp.core.io.Writer.writeTokens(outFile, tokens);
			}
		}
	}

}
