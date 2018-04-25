package slp.core.lexing.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	
	private static final int LINE_CUTOFF = 1000;

	public static void main(String[] args) throws IOException {
		File root = new File(args[0]);
		File trainDir = new File(root, "Train");
		File testDir = new File(root, "Test");
		File validDir = new File(root, "Valid");
		
		File trainOut = new File(root, "ix-train");
		File testOut = new File(root, "ix-test");
		File validOut = new File(root, "ix-valid");
		
		VocabularyRunner.read(new File("vocab.out"));
		
		writeIXs(trainDir, trainOut);
		writeIXs(testDir, testOut);
		writeIXs(validDir, validOut);
	}

	private static void writeIXs(File dir, File out) throws IOException {
		Lexer lexer = new JavaLexer();
		Vocabulary vocabulary = new Vocabulary();
		try (FileWriter fw = new FileWriter(out)) {
			List<File> files = Util.getFiles(dir);
			for (File file : files) {
				List<String> tokens = lexer.lexFile(file)
						.flatMap(vocabulary::toIndices)
						.map(ix -> "" + ix)
						.collect(Collectors.toList());
				while (tokens.size() > 1.1*LINE_CUTOFF) {
					write(fw, tokens.subList(0, LINE_CUTOFF));
					tokens = tokens.subList(LINE_CUTOFF, tokens.size());
				}
				write(fw, tokens);
			}
		}
	}

	private static void write(FileWriter fw, List<String> tokens) throws IOException {
		for (String token : tokens) {
			fw.append(token);
			fw.append(" ");
		}
		fw.append(Vocabulary.EOS);
		fw.append("\n");
	}
}
