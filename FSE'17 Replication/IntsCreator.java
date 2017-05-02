package temp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.Vocabulary;
import slp.core.io.Reader;
import slp.core.tokenizing.Tokenizer;
import slp.core.tokenizing.simple.PreTokenizer;
import slp.core.util.Util;

public class IntsCreator {
	
	public static void main(String[] args) throws IOException {
		File root = new File("Root");
		File trainDir = new File(root, "Train");
		File testDir = new File(root, "Test");
		File validDir = new File(root, "Valid");
		
		File trainDirOut = new File(trainDir.getAbsolutePath() + "-ix");
		File testDirOut = new File(testDir.getAbsolutePath() + "-ix");
		File validDirOut = new File(validDir.getAbsolutePath() + "-ix");
		
		trainDirOut.mkdir();
		testDirOut.mkdir();
		validDirOut.mkdir();
		
		File trainOut = new File(root, "ix-train");
		File testOut = new File(root, "ix-test");
		File validOut = new File(root, "ix-valid");
		
		File trainOutTrimmed = new File(root, "ix-o-train");
		File validOutTrimmed = new File(root, "ix-o-valid");
		File testOutTrimmed = new File(root, "ix-o-test");
		
		PreTokenizer tokenizer = new PreTokenizer();
		Vocabulary vocabulary = Vocabulary.fromFile(new File("vocab.out"));
		vocabulary.open();
		
		writeIXs(trainDir, trainOut, tokenizer, vocabulary);
		writeIXs(testDir, testOut, tokenizer, vocabulary);
		writeIXs(validDir, validOut, tokenizer, vocabulary);
		
		writeTrimmed(trainOut, trainOutTrimmed);
		writeTrimmed(testOut, testOutTrimmed);
		writeTrimmed(validOut, validOutTrimmed);
		
		writeIXDir(trainDir, trainDirOut, tokenizer, vocabulary);
		writeIXDir(testDir, testDirOut, tokenizer, vocabulary);
		writeIXDir(validDir, validDirOut, tokenizer, vocabulary);
	}

	private static void writeIXs(File dir, File out, PreTokenizer tokenizer, Vocabulary vocabulary) throws IOException {
		try (FileWriter fw = new FileWriter(out)) {
			List<File> files = Util.getFiles(dir);
			for (File file : files) {
				String tokens = Stream.of(Reader.readContent(file))
						.flatMap(tokenizer::tokenize)
						.map(vocabulary::toIndex)
						.map(ix -> "" + ix).collect(Collectors.joining("\t"));
				String SOL = tokens.substring(0, tokens.indexOf("\t"));
				String EOL = tokens.substring(tokens.lastIndexOf("\t") + 1);
				while (tokens.length() > 20000) {
					int i = 20000;
					for (; i < tokens.length(); i++) {
						if (tokens.charAt(i) == '\t') break;
					}
					if (i >= tokens.length() - 10) break;
					String part = tokens.substring(0, i);
					fw.append(part);
					fw.append("\t" + EOL + "\n" + SOL);
					tokens = tokens.substring(i);
				}
				fw.append(tokens);
				fw.append('\n');
			}
		}
	}
	
	private static void writeTrimmed(File inTrain, File trainOut) throws IOException {
		try (FileWriter fw = new FileWriter(trainOut)) {
			Files.lines(inTrain.toPath())
				.map(x -> {
					String[] ixs = x.split("\t");
					return Arrays.stream(ixs).skip(1).limit(ixs.length - 2)
								.collect(Collectors.joining("\t"));
				}).forEachOrdered(l -> {
					try {
						fw.append(l);
						fw.append('\n');
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
		}
	}

	private static void writeIXDir(File inDir, File outDir, Tokenizer tokenizer, Vocabulary vocabulary) throws FileNotFoundException, IOException {
		for (String child : inDir.list()) {
			File f = new File(inDir, child);
			if (f.isDirectory()) {
				File out = new File(outDir, f.getName());
				out.mkdir();
				writeIXDir(f, out, tokenizer, vocabulary);
			}
			else if (f.isFile()) {
				File outFile = new File(outDir, f.getName());
				String tokens = Stream.of(Reader.readContent(f))
						.flatMap(tokenizer::tokenize)
						.map(vocabulary::toIndex)
						.map(ix -> "" + ix).collect(Collectors.joining("\n"));
				String SOL = tokens.substring(0, tokens.indexOf("\n"));
				String EOL = tokens.substring(tokens.lastIndexOf("\n") + 1);
				int ix = 0;
				while (tokens.length() > 20000) {
					int i = 20000;
					for (; i < tokens.length(); i++) {
						if (tokens.charAt(i) == '\n') break;
					}
					if (i >= tokens.length() - 10) break;
					String part = tokens.substring(0, i);
					try (FileWriter fw = new FileWriter(outFile + "-" + ix++)) {
						fw.append(part);
						fw.append("\n" + EOL);
					}
					tokens = SOL + tokens.substring(i);
				}
				try (FileWriter fw = new FileWriter(outFile + "-" + ix)) {
					fw.append(tokens);
					fw.append('\n');
				}
			}
		}
	}
}
