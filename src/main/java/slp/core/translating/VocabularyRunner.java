package slp.core.translating;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import slp.core.io.Reader;
import slp.core.lexing.runners.LexerRunner;

public class VocabularyRunner {
	
	private static final int PRINT_FREQ = 1000000;
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
	 * Build vocabulary on all files reachable from (constructor) provided root, 
	 * possibly filtering by name/extension (see {@link #setRegex(String)}/{@link #setExtension(String)}).
	 * @return 
	 */
	public static Vocabulary build(LexerRunner lexerRunner, File root) {
		Vocabulary vocabulary = new Vocabulary();
		int[] c = { 0 };
		Map<String, Integer> counts = lexerRunner.lexDirectory(root)
			.flatMap(f -> f.right)
			.flatMap(l -> l)
			.peek(t -> {
				if (++c[0] % PRINT_FREQ == 0)
					System.out.printf("Building vocabulary, %dM tokens processed\n", Math.round(c[0]/PRINT_FREQ));
			})
			.collect(Collectors.toMap(w -> w, w -> 1, Integer::sum));
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
				vocabulary.store(token, count);
			}
		}
		vocabulary.store(Vocabulary.UNK, vocabulary.getCount(Vocabulary.UNK) + unkCount);
		if (c[0] > PRINT_FREQ) System.out.println("Vocabulary constructed on " + c[0] + " tokens, size: " + vocabulary.size());
		return vocabulary;
	}
	
	/**
	 * Read vocabulary from file, where it is assumed that the vocabulary is written as per {@link Vocabulary#write(File)}:
	 * tab-separated, having three columns per line: count, index and token (which may contain tabs))
	 * <br /><em>Note:</em>: index is assumed to be strictly incremental starting at 0!
	 * @return 
	 * @return 
	 */
	public static Vocabulary read(File file) {
		Vocabulary vocabulary = new Vocabulary();
		Reader.readLines(file).stream()
			.map(x -> x.split("\t", 3))
			.filter(x -> Integer.parseInt(x[0]) >= cutOff)
			.forEachOrdered(split -> {
				Integer count = Integer.parseInt(split[0]);
				Integer index = Integer.parseInt(split[1]);
				if (index > 0 && index != vocabulary.size()) {
					System.out.println("VocabularyRunner.read(): non-consecutive indices while reading vocabulary!");
				}
				String token = split[2];
				vocabulary.store(token, count);
			});
		return vocabulary;
	}

	/**
	 * Writes vocabulary to file with one entry per line. Format: tab-separated count, index and word.
	 * <br />
	 * Note: count is informative only and is not updated during training!
	 * 
	 * @param file File to write vocabulary to.
	 */
	public static void write(Vocabulary vocabulary, File file) {
		try (BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			for (int i = 0; i < vocabulary.size(); i++) {
				Integer count = vocabulary.getCounts().get(i);
				String word = vocabulary.getWords().get(i);
				fw.append(count + "\t" + i + "\t" + word.toString() + "\n");
			}
		} catch (IOException e) {
			System.out.println("Error writing vocabulary in Vocabulary.toFile()");
			e.printStackTrace();
		}
	}
}
