package core.translating;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import core.io.Reader;
import core.lexing.LexerRunner;
import core.util.Util;

public class VocabularyRunner {
	
	private static boolean close = false;
	private static int cutOff = 0;

	/**
	 * Set counts cut-off so that only events seen >= cutOff are considered. Default: 0, which includes every seen token
	 * <br />
	 * <em>Note:</em> this has been shown to give a distorted perspective on models of particularly source code,
	 * but may be applicable in some circumstances
	 * 
	 * @param cutOff The minimum number of counts of an event in order for it to be considered.
	 */
	public static void cutOff(int cutOff) {
		if (cutOff < 0) {
			System.out.println("VocabularyBuilder.cutOff(): negative cut-off given, set to 0 (which includes every token)");
			cutOff = 0;
		}
		VocabularyRunner.cutOff = cutOff;
	}
	
	/**
	 * Closes the vocabulary after building/reading. Default: never close, as has been shown to be more applicable
	 * to modeling (particularly) source code.
	 * <br />
	 * See also: {@link Vocabulary#close()} (and open()) to close/open vocabulary after construction
	 */
	public static void close(boolean close) {
		VocabularyRunner.close = close;
	}
	
	/**
	 * Build vocabulary on all files reachable from (constructor) provided root, 
	 * possibly filtering by name/extension (see {@link #useRegex(String)}/{@link #useExtension(String)}).
	 * @return 
	 */
	public static void build(File root) {
		List<File> files = Util.getFiles(root);
		Map<String, Integer> counts = new LinkedHashMap<>();
		int c = 0;
		for (File file : files) {
			if (++c % 1000 == 0) System.out.println("Building vocabulary, file " + c + " of " + files.size());
			LexerRunner.lex(file).stream()
				.flatMap(List::stream)
				.forEach(t -> counts.merge(t, 1, Integer::sum));
		}
		List<Entry<String, Integer>> ordered = counts.entrySet().stream()
			.sorted((e1, e2) -> -Integer.compare(e1.getValue(), e2.getValue()))
			.collect(Collectors.toList());
		int unkCount = 0;
		for (Entry<String, Integer> entry : ordered) {
			String token = entry.getKey();
			int count = entry.getValue();
			if (count < cutOff) {
				unkCount += count;
			}
			else {
				Vocabulary.store(token, count);
			}
		}
		Vocabulary.store(Vocabulary.UNK, unkCount);
		if (close) Vocabulary.close();
	}
	
	/**
	 * Read vocabulary from file, where it is assumed that the vocabulary is written as per {@link Vocabulary#write(File)}:
	 * tab-separated, having three columns per line: count, index and token (which may contain tabs))
	 * <br /><em>Note:</em>: index is assumed to be strictly incremental starting at 0!
	 * @return 
	 */
	public static void read(File file) {
		Reader.readLines(file).stream()
			.map(x -> x.split("\t", 3))
			.filter(x -> Integer.parseInt(x[0]) >= cutOff)
			.forEachOrdered(split -> {
				Integer count = Integer.parseInt(split[0]);
				Integer index = Integer.parseInt(split[1]);
				String token = split[2];
				if (token.equals(Vocabulary.UNK)) {
					Vocabulary.counts.set(0, count);
				}
				else {
					Vocabulary.counts.add(count);
					Vocabulary.words.add(token);
					Vocabulary.wordIndices.put(token, index);
				}
			});
		if (close) Vocabulary.close();
	}

	/**
	 * Writes vocabulary to file with one entry per line. Format: tab-separated count, index and word.
	 * <br />
	 * Note: count is informative only and is not updated during training!
	 * 
	 * @param file File to write vocabulary to.
	 */
	public static void write(File file) {
		try (BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			for (int i = 0; i < Vocabulary.size(); i++) {
				Integer count = Vocabulary.counts.get(i);
				String word = Vocabulary.words.get(i);
				fw.append(count + "\t" + i + "\t" + word.toString() + "\n");
			}
		} catch (IOException e) {
			System.out.println("Error writing vocabulary in Vocabulary.toFile()");
			e.printStackTrace();
		}
	}
}
