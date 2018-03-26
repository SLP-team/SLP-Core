package slp.core.lexing.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import slp.core.lexing.Lexer;
import slp.core.lexing.code.JavaLexer;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;
import slp.core.util.Util;

/**
 * Including for legacy reasons only!
 * 
 * @author Vincent Hellendoorn
 *
 */
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
		
		Lexer lexer = new JavaLexer();
		VocabularyRunner.read(new File("vocab.out"));
		
		writeIXs(trainDir, trainOut, lexer);
		writeIXs(testDir, testOut, lexer);
		writeIXs(validDir, validOut, lexer);
		
		writeTrimmed(trainOut, trainOutTrimmed);
		writeTrimmed(testOut, testOutTrimmed);
		writeTrimmed(validOut, validOutTrimmed);
		
		writeIXDir(trainDir, trainDirOut, lexer);
		writeIXDir(testDir, testDirOut, lexer);
		writeIXDir(validDir, validDirOut, lexer);
	}

	private static void writeIXs(File dir, File out, Lexer lexer) throws IOException {
		try (FileWriter fw = new FileWriter(out)) {
			List<File> files = Util.getFiles(dir);
			for (File file : files) {
				String tokens = lexer.lex(file)
						.flatMap(Vocabulary::toIndices)
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

	private static void writeIXDir(File inDir, File outDir, Lexer lexer) throws FileNotFoundException, IOException {
		for (File file : inDir.listFiles()) {
			if (file.isDirectory()) {
				File out = new File(outDir, file.getName());
				out.mkdir();
				writeIXDir(file, out, lexer);
			}
			else if (file.isFile()) {
				File outFile = new File(outDir, file.getName());
				String tokens = lexer.lex(file)
						.flatMap(Vocabulary::toIndices)
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
