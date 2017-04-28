package slp.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.counting.Counter;
import slp.core.counting.giga.GigaCounter;
import slp.core.counting.io.CountsReader;
import slp.core.counting.io.CountsWriter;
import slp.core.counting.trie.TrieCounterData;
import slp.core.example.JavaRunner;
import slp.core.example.NLRunner;
import slp.core.lexing.Lexer;
import slp.core.lexing.LexerRunner;
import slp.core.lexing.code.JavaLexer;
import slp.core.lexing.simple.CharacterLexer;
import slp.core.lexing.simple.PunctuationLexer;
import slp.core.lexing.simple.TokenizedLexer;
import slp.core.lexing.simple.WhitespaceLexer;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.mix.InverseMixModel;
import slp.core.modeling.mix.NestedModel;
import slp.core.modeling.ngram.ADMModel;
import slp.core.modeling.ngram.ADModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.ngram.NGramCache;
import slp.core.modeling.ngram.NGramModel;
import slp.core.modeling.ngram.WBModel;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;
import slp.core.util.Pair;

/**
 * Provides a command line interface to a runnable jar produced from this source code.
 * Using SLP_Core as a library can give access to more powerful usage;
 * see {@link JavaRunner} and {@link NLRunner} to get started.
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
	
	public static void main(String[] args) {
		arguments = args;
		if (arguments.length == 0 || isSet(HELP)) {
			if (arguments.length == 0) System.out.println("No arguments provided, printing help menu.");
			printHelp();
			return;
		}
		
		setupLexerRunner();
		setupModelRunner();
		if (isSet(VOCABULARY)) loadVocabulary();
		if (isSet(CLOSED)) Vocabulary.close();
		if (isSet(ORDER)) ModelRunner.setNGramOrder(Integer.parseInt(getArg(ORDER)));

		String mode = arguments[0];
		switch (mode.toLowerCase()) {
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
				System.out.println("Command " + mode + " not recognized; use -h for help.");
			}
		}
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
		System.out.println("\ttrain-test --train <path> --test <path> [OPTIONS]: train on in-path and test modeling accuracy on out-path without storing a counter");
		System.out.println("\tpredict --test <path> --counter <counts-file> [OPTIONS]: test predictions on files in in-path using counter from counts-file."
				+ "\n\t\tUse -m (below) to set the model. See also: test, train-predict");
		System.out.println("\ttrain-predict --train <path> --test <path> [OPTIONS]: train on in-path and test prediction accuracy on out-path without storing a counter");
		
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
				+ "\n\t\tHas no effect if vocabulary is build instead of read.");
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

	private static void setupLexerRunner() {
		LexerRunner.setLexer(getLexer());
		if (isSet(PER_LINE)) LexerRunner.perLine(true);
		if (isSet(ADD_DELIMS)) LexerRunner.addSentenceMarkers(true);
		if (isSet(EXTENSION)) LexerRunner.useExtension(getArg(EXTENSION));
	}

	private static Lexer getLexer() {
		String language = getArg(LANGUAGE);
		Lexer lexer;
		if (language == null || language.toLowerCase().equals("simple")) lexer = new PunctuationLexer();
		else if (language.toLowerCase().equals("java")) lexer = new JavaLexer();
		else if (language.toLowerCase().equals("tokens")) lexer = new TokenizedLexer();
		else if (language.toLowerCase().equals("blank")) lexer = new WhitespaceLexer();
		else if (language.toLowerCase().equals("char")) lexer = new CharacterLexer();
		else lexer = new PunctuationLexer();
		System.out.println("Using lexer " + lexer.getClass().getSimpleName());
		return lexer;
	}

	private static void setupModelRunner() {
		if (isSet(PER_LINE)) ModelRunner.perLine(true);
		if (isSet(TRAIN) && isSet(TEST) && getArg(TRAIN).equals(getArg(TEST))) ModelRunner.selfTesting(true);
		else if ((isSet(TRAIN) ^ isSet(TEST)) && isSet(SELF)) ModelRunner.selfTesting(true);
	}

	private static Model getModel() {
		return wrapModel(getNGramModel());
	}

	private static Model wrapModel(Model m) {
		if (isSet(NESTED) && isSet(TEST)) m = new NestedModel(new File(getArg(TEST)), m);
		if (isSet(CACHE)) m = new InverseMixModel(m, new NGramCache());
		if (isSet(DYNAMIC)) m.setDynamic(true);
		return m;
	}
	
	private static NGramModel getNGramModel() {
		Counter counter = isSet(COUNTER) ? CountsReader.readCounter(new File(getArg(COUNTER))) :
			isSet(GIGA) ? new GigaCounter() : new JMModel().getCounter();
		String modelName = getArg(MODEL);
		NGramModel model;
		if (modelName == null) model = new JMModel(counter);
		else if (modelName.toLowerCase().equals("jm")) model = new JMModel(counter);
		else if (modelName.toLowerCase().equals("wb")) model = new WBModel(counter);
		else if (modelName.toLowerCase().equals("ad")) model = new ADModel(counter);
		else if (modelName.toLowerCase().equals("adm")) model = new ADMModel(counter);
		else model = new JMModel(counter);
		NGramModel.setStandard(model.getClass());
		if (model instanceof JMModel || model instanceof WBModel) TrieCounterData.COUNT_OF_COUNTS_CUTOFF = 1;
		return model;
	}

	private static void loadVocabulary() {
		String file = getArg(VOCABULARY);
		if (file == null || file.isEmpty() || !new File(file).exists()) return;
		if (isSet(CLOSED)) VocabularyRunner.close(true);
		VocabularyRunner.read(new File(file));
	}

	private static void lex() {
		lex(false);
	}
	
	private static void lex(boolean translate) {
		if (arguments.length >= 3) {
			File inDir = new File(arguments[1]);
			File outDir = new File(arguments[2]);
			if (!outDir.exists()) outDir.mkdirs();
			LexerRunner.preTranslate(translate);
			boolean emptyVocab = Vocabulary.size() <= 1;
			LexerRunner.lexDirectory(inDir, outDir);
			if (translate && emptyVocab) {
				File vocabFile = isSet(VOCABULARY) ? new File(getArg(VOCABULARY)) : new File(outDir.getParentFile(), "train.vocab");
				VocabularyRunner.write(vocabFile);
			}
		}
		else {
			System.err.println("Not enough arguments given."
					+ "Lexing requires at least two arguments: source and target path.");
		}
	}

	private static void buildVocabulary() {
		if (arguments.length >= 3) {
			File inDir = new File(arguments[1]);
			File outFile = new File(arguments[2]);
			if (!inDir.exists()) {
				System.err.println("Source path for building vocabulary does not exist: " + inDir);
				return;
			}
			if (isSet(UNK_CUTOFF)) VocabularyRunner.cutOff(Integer.parseInt(getArg(UNK_CUTOFF)));
			VocabularyRunner.build(inDir);
			VocabularyRunner.write(outFile);
		}
		else {
			System.err.println("Not enough arguments given."
					+ "Building vocabulary requires at least two arguments: source path and output file.");
		}
	}

	private static void train() {
		if (arguments.length >= 5) {
			File inDir = new File(getArg(TRAIN));
			File outFile = new File(getArg(COUNTER));
			if (!inDir.exists()) {
				System.err.println("Source path for training does not exist: " + inDir);
				return;
			}
			boolean emptyVocab = Vocabulary.size() <= 1;
			NGramModel model = getNGramModel();
			ModelRunner.learn(model, inDir);
			// Since this is for training n-grams only, override ModelRunner's model for easy access to the counter
			Counter counter = model.getCounter();
			CountsWriter.writeCounter(counter, outFile);
			if (emptyVocab) {
				File vocabFile = isSet(VOCABULARY) ? new File(getArg(VOCABULARY)) : new File(outFile.getParentFile(), "train.vocab");
				VocabularyRunner.write(vocabFile);
			}
		}
		else {
			System.err.println("Not enough arguments given."
					+ "Training requires at least two arguments: source path and output file.");
		}
	}

	private static void test() {
		if (arguments.length >= 5) {
			File inDir = new File(getArg(TEST));
			File counterFile = new File(getArg(COUNTER));
			if (!inDir.exists()) {
				System.err.println("Test path does not exist: " + inDir);
			} else if (!counterFile.exists()) {
				System.err.println("Counter file to read in not found: " + inDir);
			}
			else {
				Map<File, List<List<Double>>> fileProbs = ModelRunner.model(getModel(), inDir).collect(Collectors.toMap(p -> p.left, p -> p.right));
				DoubleSummaryStatistics stats = ModelRunner.getStats(fileProbs);
				System.out.printf("Testing complete, modeled %d files with %d tokens yielding average entropy:\t%.4f\n",
						fileProbs.size(), stats.getCount(), stats.getAverage());
				write(fileProbs);
			}
		}
		else {
			System.err.println("Not enough arguments given."
					+ "Testing requires at least two arguments: test path and counter file.");
		}
	}

	private static void trainTest() {
		if (arguments.length >= 3) {
			File trainDir = new File(getArg(TRAIN));
			File testDir = new File(getArg(TEST));
			if (!trainDir.exists()) {
				System.err.println("Source path for training does not exist: " + trainDir);
				return;
			}
			else if (!testDir.exists()) {
				System.err.println("Source path for testing does not exist: " + testDir);
				return;
			}
			NGramModel nGramModel = getNGramModel();
			ModelRunner.learn(nGramModel, trainDir);
			Model model = wrapModel(nGramModel);
			Map<File, List<List<Double>>> fileProbs = ModelRunner.model(model, testDir).collect(Collectors.toMap(p -> p.left, p -> p.right));
			DoubleSummaryStatistics stats = ModelRunner.getStats(fileProbs);
			System.out.printf("Testing complete, modeled %d files with %d tokens yielding average entropy:\t%.4f\n",
					fileProbs.size(), stats.getCount(), stats.getAverage());
			write(fileProbs);
		}
		else {
			System.err.println("Not enough arguments given."
					+ "train-testing requires at least two arguments: train path and test path.");
		}
	}

	private static void predict() {
		if (arguments.length >= 3) {
			File inDir = new File(getArg(TEST));
			File counterFile = new File(getArg(COUNTER));
			if (!inDir.exists()) {
				System.err.println("Test path does not exist: " + inDir);
			} else if (!counterFile.exists()) {
				System.err.println("Counter file to read in not found: " + inDir);
			}
			else {
				Map<File, List<List<Double>>> fileMRRs = ModelRunner.predict(getModel(), inDir).collect(Collectors.toMap(p -> p.left, p -> p.right));
				DoubleSummaryStatistics stats = ModelRunner.getStats(fileMRRs);
				System.out.printf("Testing complete, modeled %d files with %d tokens yielding average MRR:\t%.4f\n",
						fileMRRs.size(), stats.getCount(), stats.getAverage());
				write(fileMRRs);
			}
		}
		else {
			System.err.println("Not enough arguments given."
					+ "Predicting requires two positional arguments: test path and counter file.");
		}		
	}

	private static void trainPredict() {
		if (arguments.length >= 3) {
			File trainDir = new File(getArg(TRAIN));
			File testDir = new File(getArg(TEST));
			if (!trainDir.exists()) {
				System.err.println("Source path for training does not exist: " + trainDir);
				return;
			}
			else if (!testDir.exists()) {
				System.err.println("Source path for testing does not exist: " + testDir);
				return;
			}
			NGramModel nGramModel = getNGramModel();
			ModelRunner.learn(nGramModel, trainDir);
			Model model = wrapModel(nGramModel);
			Map<File, List<List<Double>>> fileMRRs = ModelRunner.predict(model, testDir).collect(Collectors.toMap(p -> p.left, p -> p.right));
			DoubleSummaryStatistics stats = ModelRunner.getStats(fileMRRs);
			System.out.printf("Testing complete, modeled %d files with %d tokens yielding average MRR:\t%.4f\n",
					fileMRRs.size(), stats.getCount(), stats.getAverage());
			write(fileMRRs);
		}
		else {
			System.err.println("Not enough arguments given."
					+ "train-predicting requires two positional arguments: train path and test path.");
		}		
	}

	static boolean isSet(String arg) {
		for (String a : arguments) {
			if (a.matches(arg)) return true;
		}
		return false;
	}

	static String getArg(String arg) {
		for (int i = 1; i < arguments.length; i++) {
			String a = arguments[i];
			if (a.matches(arg)) {
				if (i < arguments.length - 1) return arguments[i + 1];
				return "";
			}
		}
		return null;
	}

	private static void write(Map<File, List<List<Double>>> fileProbs) {
		String out = getArg(VERBOSE);
		if (out != null) {
			try (FileWriter fw = new FileWriter(new File(out))) {
				for (Entry<File, List<List<Double>>> entry : fileProbs.entrySet()) {
					fw.append(entry.getKey() + "\n");
					for (List<Double> line : entry.getValue()) {
						for (int i = 0; i < line.size(); i++) {
							Double d = line.get(i);
							fw.append(String.format("%.6f", d));
							if (i < line.size() - 1) fw.append('\t');
							else fw.append('\n');
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
