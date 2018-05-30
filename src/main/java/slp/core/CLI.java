package slp.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.giga.GigaCounter;
import slp.core.counting.io.CounterIO;
import slp.core.counting.trie.AbstractTrie;
import slp.core.example.BasicJavaRunner;
import slp.core.example.BasicNLRunner;
import slp.core.io.Writer;
import slp.core.lexing.Lexer;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.runners.LexerRunner;
import slp.core.lexing.simple.CharacterLexer;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.lexing.simple.TokenizedLexer;
import slp.core.lexing.simple.WhitespaceLexer;
import slp.core.modeling.Model;
import slp.core.modeling.dynamic.CacheModel;
import slp.core.modeling.dynamic.NestedModel;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.ADMModel;
import slp.core.modeling.ngram.ADModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.ngram.NGramModel;
import slp.core.modeling.ngram.WBModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;
import slp.core.util.Pair;

/**
 * Provides a command line interface to a runnable jar produced from this source code.
 * Using SLP-Core as a library can give access to more powerful usage;
 * see {@link BasicJavaRunner} and {@link BasicNLRunner} to get started.
 * 
 * @author Vincent Hellendoorn
 *
 */
public class CLI {

	// General options
	private static final String HELP = "(-h|--help)";
	private static final String VERBOSE = "--verbose";

	// Lexing options
	private static final String LANGUAGE = "(-l|--language)";
	private static final String ADD_DELIMS = "--delims";
	private static final String PER_LINE = "(-pl|--per-line)";
	private static final String EXTENSION = "(-e|--extension)";

	// Vocabulary options	
	private static final String VOCABULARY = "(-v|--vocabulary)";
	private static final String CLOSED = "--closed";
	private static final String UNK_CUTOFF = "(-u|--unk-cutoff)";

	// Training options
	private static final String TRAIN = "(-tr|--train)";
	private static final String ORDER = "(-o|--order)";
	private static final String GIGA = "--giga";
	
	// Testing options
	private static final String TEST = "(-te|--test)";
	private static final String COUNTER = "--counter";
	private static final String MODEL = "(-m|--model)";
	private static final String SELF = "(-s|--self)";
	private static final String CACHE = "(-c|--cache)";
	private static final String DYNAMIC = "(-d|--dynamic)";
	private static final String NESTED = "(-n|--nested)";

	private static String[] arguments;
	private static String mode;

	private static Lexer lexer;
	private static Vocabulary vocabulary;
	private static Model model;
	private static LexerRunner lexerRunner;
	private static ModelRunner modelRunner;
	
	public static FileWriter logger;
	
	public static void main(String[] args) {
		arguments = args;
		if (arguments.length == 0 || isSet(HELP)) {
			if (arguments.length == 0) System.out.println("No arguments provided, printing help menu.");
			printHelp();
			return;
		}
		mode = arguments[0].toLowerCase();
		
		lexer = getLexer();
		lexerRunner = setupLexerRunner();
		vocabulary = setupVocabulary();
		model = getModel();
		modelRunner = setupModelRunner();
		setupLogger();
		switch (mode) {
			case "lex": {
				lex(); break;
			}
			case "lex-ix": {
				lex(true); break;
			}
			case "vocabulary": {
				buildVocabulary(); break;
			}
			case "train": {
				train(); break;
			}
			case "test": {
				test(); break;
			}
			case "train-test": {
				trainTest(); break;
			}
			case "predict": {
				predict(); break;
			}
			case "train-predict": {
				trainPredict(); break;
			}
			default: {
				exit("Command " + mode + " not recognized; use -h for help.");
			}
		}
		teardownLogger();
	}

	private static void printHelp() {
		System.out.println("\nMain API for command-line usage of SLP-core: can be used to lex corpora, build vocabularies and train/test/predict."
				+ "\nAll models are n-gram models at present for the Jar, for more powerful usage, use as library in Java code."
				+ "\nEvery use-case starts with lexing, so make sure those options are set correctly.");
		
		System.out.println("\nUsage:");
		System.out.println("\t-h | --help (or no arguments): Print this help menu)");
		System.out.println("\tlex <in-path> <out-path> [OPTIONS]: lex the in-path (file or directory) to a mirrored structure in out-path."
				+ "\n\t\tSee lexing options below, for instance to specify extension filters, delimiter options.");
		System.out.println("\tlex-ix <in-path> <out-path> [OPTIONS]: like lex, excepts translates tokens to integers."
				+ "\n\t\tNote: if not given a vocabulary, will build one first and write it to 'train.vocab' in same dir as out-path");
		System.out.println("\tvocabulary <in-path> <out-file> [OPTIONS]: lex the in-path and write resulting vocabulary to out-file.");
		System.out.println("\ttrain --train <path> --counter <out-file> [OPTIONS]: lex all files in in-path, train n-gram model and write to out-file."
				+ "\n\t\tCurrently the Jar supports n-gram models only; config-file support may come in further revisions."
				+ "\n\t\tNote: if not given a vocabulary, will build one first and write it to 'train.vocab' in same dir as out-file");
		System.out.println("\ttest --test <path> --counter <counts-file> -v <vocab-file> [OPTIONS]: test on files in in-path using counter from counts-file."
				+ "\n\t\tNote that the vocabulary parameter is mandatory; a counter is meaningless without a vocabulary."
				+ "\n\t\tUse -m (below) to set the model. See also: predict, train-test.");
		System.out.println("\ttrain-test [--train <path>] --test <path> [OPTIONS]: train on train-path or self-test on test-path; tests modeling accuracy on test-path without storing a counter");
		System.out.println("\tpredict --test <path> --counter <counts-file> [OPTIONS]: test predictions on files in in-path using counter from counts-file."
				+ "\n\t\tUse -m (below) to set the model. See also: test, train-predict");
		System.out.println("\ttrain-predict --train <path> --test <path> [OPTIONS]: train on train-path or self-test on test-path; tests prediction accuracy on test-path without storing a counter");
		
		System.out.println("\nOptions:");
		System.out.println("  General:");
		System.out.println("\t-h | --help: Show this screen");
		System.out.println("\t--verbose <file>: print all output to file");
		System.out.println("  Lexing:");
		System.out.println("\t-l | --language: specify language for tokenization. Currently one of (simple, blank, tokens, java)."
				+ "\n\t\t Use 'simple' (default) for splitting on punctuation (preserved as tokens) and whitespace (ignored);"
				+ "\n\t\t use 'blank' for just splitting on whitespace and use 'tokens' for pre-tokenized text.");
		System.out.println("\t--delims: explicitly add line delimiters to the start and end of the input. Default: none"
				+ "\n\t\tWill add to every line if --per-line is set");
		System.out.println("\t-pl | --per-line: lex and model each line in isolation. Default: false");
		System.out.println("\t-e | --extension: use the provided extension regex to filter input files. Default: none filtered");
		System.out.println("  Vocabulary:");
		System.out.println("\t-v | --vocabulary: specify file to read vocabulary from."
				+ "\n\t\tIf none given, vocabulary is constructed 'on the fly' while modeling.");
		System.out.println("\t--closed: close vocabulary after reading, treating every further token as unknown."
				+ "\n\t\tNot generally recommended for source code, but sometimes applicable."
				+ "\n\t\tHas no effect if vocabulary is built on-line instead of read from file.");
		System.out.println("\t-u | --unk-cutoff: set an unknow token cut-off when building/reading in vocabulary."
				+ "\n\t\tAll tokens seen >= cut-off times are preserved. Default: 0, preserving all tokens.");
		System.out.println("  Training:");
		System.out.println("\t-tr | --train: the path to train on");
		System.out.println("\t-o | --order: specify order for n-gram models. Default: 6");
		System.out.println("  Testing:");
		System.out.println("\t-te | --test: the path to test on");
		System.out.println("\t--counter: the path to read the counter from, if testing with pre-trained model");
		System.out.println("\t-m | --model: use specified n-gram smoothing model:"
				+ "\n\t\tjm = Jelinek-Mercer, wb = Witten-Bell, ad(m) = (modified) absolute discounting");
		System.out.println("\t-s | --self: specify that we are testing on the train data, implying to 'forget' any data prior to testing.");
		System.out.println("\t-c | --cache: add an n-gram cache model");
		System.out.println("\t-d | --dynamic: dynamically update all models with test data");
		System.out.println("\t-n | --nested: build a nested model of test data (sets dynamic to false); see paper for more details");
		System.out.println();
	}
	
	private static void exit(String message) {
		System.err.println(message);
		teardownLogger();
		System.exit(1);
	}

	private static LexerRunner setupLexerRunner() {
		LexerRunner lexerRunner = new LexerRunner(lexer, isSet(PER_LINE));
		if (isSet(ADD_DELIMS)) lexerRunner.setSentenceMarkers(true);
		if (isSet(EXTENSION)) lexerRunner.setExtension(getArg(EXTENSION));
		return lexerRunner;
	}

	private static Vocabulary setupVocabulary() {
		Vocabulary vocabulary;
		if (isSet(VOCABULARY)) {
			String file = getArg(VOCABULARY);
			if (file == null || file.isEmpty() || !new File(file).exists())
				exit("Vocabulary file not found: " + file);
			if (isSet(UNK_CUTOFF))
				VocabularyRunner.cutOff(Integer.parseInt(getArg(UNK_CUTOFF)));
			System.out.println("Retrieving vocabulary from file");
			vocabulary = VocabularyRunner.read(new File(file));
		}
		else vocabulary = new Vocabulary();
		if (isSet(CLOSED)) vocabulary.close();
		return vocabulary;
	}

	private static ModelRunner setupModelRunner() {
		ModelRunner modelRunner = new ModelRunner(model, lexerRunner, vocabulary);
		if (isSelf()) modelRunner.setSelfTesting(true);
		return modelRunner;
	}

	private static Lexer getLexer() {
		Lexer lexer;
		String language = getArg(LANGUAGE);
		if (language == null || language.toLowerCase().equals("simple")) lexer = new PunctuationLexer();
		else if (language.toLowerCase().equals("java")) lexer = new JavaLexer();
		else if (language.toLowerCase().equals("tokens")) lexer = new TokenizedLexer();
		else if (language.toLowerCase().equals("blank")) lexer = new WhitespaceLexer();
		else if (language.toLowerCase().equals("char")) lexer = new CharacterLexer();
		else lexer = new PunctuationLexer();
		System.out.println("Using lexer " + lexer.getClass().getSimpleName());
		return lexer;
	}

	private static void setupLogger() {
		if (isSet(VERBOSE)) {
			try {
				logger = new FileWriter(getArg(VERBOSE));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void teardownLogger() {
		if (logger != null) {
			try {
				logger.close();
			} catch (IOException e) {
			}
		}
	}

	private static Model getModel() {
		return wrapModel(getNGramModel());
	}

	private static NGramModel getNGramModel() {
		Counter counter = getCounter();
		int order = isSet(ORDER) ? Integer.parseInt(getArg(ORDER)) : ModelRunner.DEFAULT_NGRAM_ORDER;
		String modelName = getArg(MODEL);
		NGramModel model;
		if (modelName == null) model = new JMModel(order, counter);
		else if (modelName.toLowerCase().equals("jm")) model = new JMModel(order, counter);
		else if (modelName.toLowerCase().equals("wb")) model = new WBModel(order, counter);
		else if (modelName.toLowerCase().equals("ad")) model = new ADModel(order, counter);
		else if (modelName.toLowerCase().equals("adm")) model = new ADMModel(order, counter);
		else model = new JMModel(order, counter);
		NGramModel.setStandard(model.getClass());
		// This will speed up and reduce memory of trie counting for the simplest classes of models
		if (model instanceof JMModel || model instanceof WBModel) AbstractTrie.COUNT_OF_COUNTS_CUTOFF = 1;
		return model;
	}

	private static Counter getCounter() {
		if (!mode.equals("test") && !mode.equals("predict")) {
			return isSet(GIGA) ? new GigaCounter() : new JMModel().getCounter();
		}
		else {
			File counterFile = getCounterFile();
			if (!counterFile.exists()) {
				exit("No (valid) counter file given for test/predict mode! Specify one with --counter *path-to-counter*");
			}
			else {
				long t = System.currentTimeMillis();
				System.out.println("Retrieving counter from file");
				Counter counter = CounterIO.readCounter(new File(getArg(COUNTER)));
				System.out.println("Counter retrieved in " + (System.currentTimeMillis() - t)/1000 + "s");
				return counter;
			}
		}
		return null;
	}

	private static Model wrapModel(Model m) {
		if (isSet(NESTED)) {
			if (isSet(TEST)) m = new NestedModel(m, lexerRunner, vocabulary, new File(getArg(TEST)));
			else exit("Nested mode set, but no test directory given!");
		}
		if (isSet(CACHE)) m = MixModel.standard(m, new CacheModel());
		if (isSet(DYNAMIC)) m.setDynamic(true);
		return m;
	}

	private static void lex() {
		lex(false);
	}
	
	private static void lex(boolean translate) {
		if (arguments.length >= 3) {
			File inDir = new File(arguments[1]);
			File outDir = new File(arguments[2]);
			boolean emptyVocab = vocabulary.size() <= 1;
			if (!translate) lexerRunner.lexDirectory(inDir, outDir);
			else {
				lexerRunner.lexDirectoryToIndices(inDir, outDir, vocabulary);
				// We also write the vocabulary if it was empty; otherwise the indices are meaningless
				if (emptyVocab) {
					File vocabFile = getVocabularyFile();
					if (!vocabFile.exists()) vocabFile = new File(outDir.getParentFile(), "train.vocab");
					System.out.println("Writing vocabulary to: " + vocabFile);
					VocabularyRunner.write(vocabulary, vocabFile);
				}
			}
		}
		else {
			exit("Not enough arguments given."
					+ "Lexing requires at least two arguments: source and target path.");
		}
	}

	private static void buildVocabulary() {
		if (arguments.length >= 3) {
			File inDir = new File(arguments[1]);
			File outFile = new File(arguments[2]);
			if (!inDir.exists()) {
				exit("Source path for building vocabulary does not exist: " + inDir);
			}
			if (isSet(UNK_CUTOFF)) VocabularyRunner.cutOff(Integer.parseInt(getArg(UNK_CUTOFF)));
			vocabulary = VocabularyRunner.build(lexerRunner, inDir);
			VocabularyRunner.write(vocabulary, outFile);
		}
		else {
			exit("Not enough arguments given."
					+ "Building vocabulary requires at least two arguments: source path and output file.");
		}
	}

	private static void train() {
		File inDir = getTrainFile();
		File outFile = new File(getArg(COUNTER));
		if (inDir == null || !inDir.exists()) {
			exit("Source path for training does not exist: " + inDir);
		}
		boolean emptyVocab = vocabulary.size() <= 1;
		modelRunner.learnDirectory(inDir);
		Counter counter = ((NGramModel) model).getCounter();
		// Force GigaCounter.resolve() (if applicable), just for accurate timings below
		counter.getCount();
		
		long t = System.currentTimeMillis();
		System.out.println("Writing counter to file");
		CounterIO.writeCounter(counter, outFile);
		System.out.println("Counter written in " + (System.currentTimeMillis() - t)/1000 + "s");
		if (emptyVocab) {
			System.out.println("Writing vocabulary to file");
			File vocabFile = getVocabularyFile();
			if (!vocabFile.exists()) vocabFile = new File(outFile.getParentFile(), "train.vocab");
			VocabularyRunner.write(vocabulary, vocabFile);
			System.out.println("Vocabulary written");
		}
	}

	private static void test() {
		File inDir = getTestFile();
		File counterFile = getCounterFile();
		if (inDir == null || !inDir.exists()) {
			exit("Test path does not exist: " + getArg(TEST));
		} else if (counterFile == null || !counterFile.exists()) {
			exit("Counter file to use not found: " + getArg(COUNTER));
		}
		
		Stream<Pair<File, List<List<Double>>>> fileProbs = modelRunner.modelDirectory(inDir);
		int[] fileCount = { 0 };
		DoubleSummaryStatistics stats = modelRunner.getStats(
				fileProbs.peek(f -> Writer.writeEntropies(lexerRunner, f))
				.peek(f -> fileCount[0]++));
		System.out.printf("Testing complete, modeled %d files with %d tokens yielding average entropy:\t%.4f\n",
				fileCount[0], stats.getCount(), stats.getAverage());
	}

	private static void trainTest() {
		trainModel();
		File testDir = getTestFile();
		Stream<Pair<File, List<List<Double>>>> fileProbs = modelRunner.modelDirectory(testDir);
		int[] fileCount = { 0 };
		DoubleSummaryStatistics stats = modelRunner.getStats(
				fileProbs.peek(f -> Writer.writeEntropies(lexerRunner, f))
				.peek(f -> fileCount[0]++));
		System.out.printf("Testing complete, modeled %d files with %d tokens yielding average entropy:\t%.4f\n",
				fileCount[0], stats.getCount(), stats.getAverage());
	}

	private static void predict() {
		File inDir = getTestFile();
		File counterFile = getCounterFile();
		if (inDir == null || !inDir.exists()) {
			exit("Test path does not exist: " + getArg(TEST));
		} else if (counterFile == null || !counterFile.exists()) {
			exit("Counter file to use not found: " + getArg(COUNTER));
		}
		
		Stream<Pair<File, List<List<Double>>>> fileMRRs = modelRunner.predictDirectory(inDir);
		int[] fileCount = { 0 };
		DoubleSummaryStatistics stats = modelRunner.getStats(
				fileMRRs.peek(f -> Writer.writeEntropies(lexerRunner, f))
				.peek(f -> fileCount[0]++));
		System.out.printf("Testing complete, modeled %d files with %d tokens yielding average MRR:\t%.4f\n",
				fileCount[0], stats.getCount(), stats.getAverage());
	}

	private static void trainPredict() {
		trainModel();
		File testDir = getTestFile();
		Stream<Pair<File, List<List<Double>>>> fileMRRs = modelRunner.predictDirectory(testDir);
		int[] fileCount = { 0 };
		DoubleSummaryStatistics stats = modelRunner.getStats(
				fileMRRs.peek(f -> Writer.writeEntropies(lexerRunner, f))
				.peek(f -> fileCount[0]++));
		System.out.printf("Testing complete, modeled %d files with %d tokens yielding average MRR:\t%.4f\n",
				fileCount[0], stats.getCount(), stats.getAverage());
	}

	private static void trainModel() {
		File trainDir = getTrainFile();
		File testDir = getTestFile();
		if (trainDir == null || !trainDir.exists()) {
			System.out.println("No valid train path given, will assume self-testing on test data");
			modelRunner.setSelfTesting(true);
		}
		else if (testDir == null || !testDir.exists()) {
			exit("No valid test path given for train-test mode, exiting");
		}
		if (trainDir != null) modelRunner.learnDirectory(trainDir);
	}

	private static boolean isSet(String arg) {
		for (String a : arguments) {
			if (a.matches(arg)) return true;
		}
		return false;
	}

	private static String getArg(String arg) {
		for (int i = 1; i < arguments.length; i++) {
			String a = arguments[i];
			if (a.matches(arg)) {
				if (i < arguments.length - 1) return arguments[i + 1];
				return "";
			}
		}
		return null;
	}

	private static File getVocabularyFile() {
		return isSet(VOCABULARY) ? new File(getArg(VOCABULARY)) : null;
	}

	private static File getCounterFile() {
		return isSet(COUNTER) ? new File(getArg(COUNTER)) : null;
	}

	private static File getTrainFile() {
		return isSet(TRAIN) ? new File(getArg(TRAIN)) : null;
	}

	private static File getTestFile() {
		return isSet(TEST) ? new File(getArg(TEST)) : null;
	}

	private static boolean isSelf() {
		// Self testing if SELF has been set, if TRAIN equal to TEST, or if only test was set for train-test/train-predict
		return isSet(SELF)
				|| (isSet(TRAIN) && isSet(TEST) && getArg(TRAIN).equals(getArg(TEST)))
				|| (mode.startsWith("train-") && !isSet(TRAIN));
	}
}
