package slp.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.counting.io.CountsWriter;
import slp.core.io.Reader;
import slp.core.sequences.Sequencer;
import slp.core.tokenizing.Tokenizer;
import slp.core.util.Util;

public class Training {

	public static void train(File inDir, File outPath, Tokenizer tokenizer, Vocabulary vocabulary) throws NumberFormatException, IOException {
		boolean emptyVocab = vocabulary.size() <= 1;
		List<File> files = new ArrayList<>();
		files.add(inDir);
		if (emptyVocab) {
			vocabulary = Vocabulary.build(tokenizer, Util.getFiles(inDir));
		}
		System.out.println("Vocabulary retrieved");
		Counter counter = Counter.standard();
		train(files, tokenizer, vocabulary, counter);
		try {
			CountsWriter.writeCounter(counter, outPath);
			if (emptyVocab) Vocabulary.toFile(vocabulary, new File(outPath.getAbsolutePath() + ".vocab"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Counter train(File root, Tokenizer tokenizer, Vocabulary vocabulary) {
		return train(Util.getFiles(root), tokenizer, vocabulary);
	}

	public static Counter train(List<File> files, Tokenizer tokenizer, Vocabulary vocabulary) {
		Counter counter = Counter.standard();
		countAll(files, tokenizer, vocabulary, Sequencer.standard(), counter);
		return counter;
	}

	public static void train(List<File> files, Tokenizer tokenizer, Vocabulary vocabulary, Counter counter) {
		countAll(files, tokenizer, vocabulary, Sequencer.standard(), counter);
	}
	
	static void countAll(List<File> files, Tokenizer tokenizer, Vocabulary vocabulary, Sequencer sequencer, Counter counter) {
		if (files.size() == 1 && files.get(0).isDirectory()) {
			for (String child : files.get(0).list()) {
				File dir = new File(files.get(0), child);
				List<File> dirFiles = Util.getFiles(dir);
				countAll(dirFiles, tokenizer, vocabulary, sequencer, counter);
			}
		}
		else {
			for (int i = 0; i < files.size(); i++) {
				File file = files.get(i);
				Stream.of(Reader.readContent(file))
					.map(tokenizer::tokenize)
					.map(vocabulary::toIndices)
					.flatMap(sequencer::sequenceForward)
					.forEachOrdered(counter::addForward);
				if (files.size() < 100 || i + 1 == files.size() || (i + 1) % 1000 == 0) {
					System.out.println("Counting at file " + (i + 1) + ", tokens processed: " + counter.getCount());
				}
			}
		}
	}
}
