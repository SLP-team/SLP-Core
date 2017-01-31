package slp.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.counting.io.CountsReader;
import slp.core.modeling.CacheModel;
import slp.core.modeling.Model;
import slp.core.modeling.ngram.AbsDiscModModel;
import slp.core.modeling.ngram.AbsDiscModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.ngram.WBModel;
import slp.core.tokenizing.BlankTokenizer;
import slp.core.tokenizing.LineTokenizer;
import slp.core.tokenizing.Tokenizer;
import slp.core.tokenizing.java.JavaTokenizer;
import slp.core.util.Configuration;
import slp.core.util.Util;

public class Main {
	public static void main(String[] args) {
		args = new String[] { "train", "E:/CP/GJTok", "train.out", "-v=E:CP/vocab.out", "-o=6", "-l=tokens" };
		if (args.length == 0) {
			printHelp();
			System.out.println("No arguments provided. use -h to print help menu.");
			return;
		}
		if (isSet(args, "(-h|--help)")) {
			printHelp();
			return;
		}
		String order = getArg(args, "(-o|--order)");
		if (order != null) Configuration.setOrder(Integer.parseInt(order));
		String mode = args[0];
		switch (mode.toLowerCase()) {
			case "tokenize": {
				tokenize(args); break;
			}
			case "tokenize-ix": {
				tokenizeIx(args); break;
			}
			case "vocabulary": {
				buildVocabulary(args); break;
			}
			case "train": {
				train(args); break;
			}
			case "test": {
				test(args); break;
			}
			case "train-test": {
				trainTest(args); break;
			}
			case "predict": {
				predict(args); break;
			}
			case "train-predict": {
				trainPredict(args); break;
			}
		}
	}

	private static void printHelp() {
		System.out.println("\nMain API for command-line usage of SLP core. For more powerful usage, "
				+ "use as library in code (see ExampleRunners in slp.core.runners). "
				+ "All paths traversed recursively unless otherwise specified; all models used here are n-gram models");
		
		System.out.println("\nUsage:");
		System.out.println("\t(-h | --help: Print this help menu)");
		System.out.println("\ttokenize <in-path> <out-path> [-l=simple] [--flatten]:"
				+ "\n\t\tTokenizes files in in-path into mirror-files in out-path, or into flat indexed directory if --flatten is set."
				+ "\n\t\tPrints tokens on separate lines (newlines in tokens escaped first); set -l flag to 'tokens' to read in");
		System.out.println("\ttokenize-ix <in-path> <out-path> <vocabulary-path> [-l=simple] [--flatten]:"
				+ "\n\t\tLike tokenize, but writes indices of tokens by translating using the (closed) vocabulary provided."
				+ "\n\t\tHelpful for comparing across models without need to specify tokenizer.");
		System.out.println("\tvocabulary <in-path> <out-path> [l=simple] [unk-cutoff=0]:"
				+ "\n\t\tBuild vocabulary from all files in <in-path> and write to <out-path>."
				+ "\n\t\tunk-cutoff specifies lowest count to include.");
		System.out.println("\ttrain <in-path> <out-path> [-l=simple] [-v=empty] [-o=5]:"
				+ "\n\t\tTrains on all files in in-path using specified language."
				+ "\n\t\tIf no/empty vocabulary specified, writes corresponding vocabulary to \"<out-path>.vocab\".");
		System.out.println("\ttest <in-path> <counter-path> <vocabulary-path> [-m=wb] [-l=simple] [-c] [-o=5]:"
				+ "\n\t\tTests on all files in in-path using specified language, model and pre-trained counter."
				+ "\n\t\tPrints per-token entropy and token-count at intervals and upon completion");
		System.out.println("\ttrain-test <in-path> (<test-path>|[-s]) [-l=simple] [-m=wb] [-v=empty] [-c] [-o=5]:"
				+ "\n\t\tLike train, but tests model directly instead of writing it."
				+ "\n\t\tIf test-path argument is not a directory, acts as if -s (self-test) flag has been set");
		System.out.println("\tpredict <in-path> <counter-path> <vocabulary-path> [-m=wb] [-l=simple] [-c] [-o=5]:"
				+ "\n\t\tTests on all files in in-path using specified language, model and pre-trained counter."
				+ "\n\t\tPrints accuracy at 1, 5, 10 and MRR at intervals and upon completion");
		System.out.println("\ttrain-predict <in-path> (<test-path>|[-s]) [-l=simple] [-m=wb] [-v=empty] [-c] [-o=5]:"
				+ "\n\t\tLike train, but tests model on prediction task directly instead of writing it."
				+ "\n\t\tIf test-path argument is not a directory, acts as if -s (self-test) flag has been set");
		
		System.out.println("\nOptions:");
		System.out.println("\t-h | --help: Show this screen");
		System.out.println("\t-l | --language: specify language for tokenization. Currently one of (simple, blanl, tokens, Java)"
				+ "\n\t\t\t Use 'simple' for splitting on punctuation (preserved as tokens) and whitespace (ignored);"
				+ "\n\t\t\t use 'blank' for just splitting on whitespace and use 'tokens' for pre-tokenized text.");
		System.out.println("\t-v | --vocabulary: specify vocabulary: either file or one of \"empty\"");
		System.out.println("\t-m | --model: specify n-gram language model to use (jm = Jelinek-Mercer, wb = Witten-Bell, abs = Absolute Discounting, "
				+ "abs-mod = Absolute Discounting with 3 discounts.");
		System.out.println("\t-o | --order: Specify order for n-gram model");
		System.out.println("\t(-c | --cache): Use cache. Type -c=*size* to specify *size* of cache (in tokens), default: per file with max of 5,000.");
		System.out.println("\t(-s | --self): Get self-entropy: uncounts events prior to testing.");
		System.out.println("\t(-d | --deep): Deep modeling: recursively build a model at all file depths.");
	}

	private static void tokenize(String[] args) {
		if (args.length < 3) {
			System.err.println("Too few arguments for tokenizing (need in-path and out-path at least). Use -h for help menu");
			return;
		}
		Tokenizer tokenizer = getTokenizer(args);
		File inDir = new File(args[1]);
		File outDir = new File(args[2]);
		if (!outDir.exists()) outDir.mkdirs();
		boolean flatten = isSet(args, "--flatten");
		Tokenizing.tokenize(tokenizer, inDir, outDir, flatten);
	}
	
	private static void tokenizeIx(String[] args) {
		if (args.length < 4) {
			System.err.println("Too few arguments for tokenizing to ix (need in-path, out-path and vocabulary-path at least). Use -h for help menu");
			return;
		}
		Tokenizer tokenizer = getTokenizer(args);
		File inDir = new File(args[1]);
		File outDir = new File(args[2]);
		Vocabulary vocabulary = getVocabulary(args);
		if (!outDir.exists()) outDir.mkdirs();
		boolean flatten = isSet(args, "--flatten");
		Tokenizing.tokenize(tokenizer, inDir, outDir, vocabulary, flatten);
	}

	private static void buildVocabulary(String[] args) {
		if (args.length < 3) {
			System.err.println("Too few arguments for vocabulary (need in-path and out-path at least). Use -h for help menu");
			return;
		}
		List<File> files = new ArrayList<>();
		File inFile = new File(args[1]);
		if (inFile.isFile()) files.add(inFile);
		else files.addAll(Util.getFiles(inFile));
		File vocabFile = new File(args[2]);
		Tokenizer tokenizer = getTokenizer(args);
		int cutOff = args.length > 4 ? Integer.parseInt(args[4]) : 0;
		try {
			Vocabulary vocabulary = Vocabulary.build(tokenizer, cutOff, files.toArray(new File[files.size()]));
			Vocabulary.toFile(vocabulary, vocabFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void train(String[] args) {
		if (args.length < 3) {
			System.err.println("Too few arguments for training (need in-path and out-path at least). Use -h for help menu");
			return;
		}
		File inDir = new File(args[1]);
		File outPath = new File(args[2]);
		Tokenizer tokenizer = getTokenizer(args);
		Vocabulary vocabulary = getVocabulary(args);
		try {
			Training.train(inDir, outPath, tokenizer, vocabulary);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	private static void test(String[] args) {
		if (args.length < 4) {
			System.err.println("Too few arguments for testing (need in-path, out-path and vocabulary-path at least). Use -h for help menu");
			return;
		}
		File inDir = new File(args[1]);
		try {
			Counter counter = CountsReader.readCounter(new File(args[2]));
			Vocabulary vocabulary = Vocabulary.fromFile(new File(args[3]));
			Tokenizer tokenizer = getTokenizer(args);
			Model model = getModel(args, counter);
			Testing.test("model", inDir, vocabulary, tokenizer, counter, model, useCache(args), self(args), deep(args));
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	private static void trainTest(String[] args) {
		if (args.length < 2) {
			System.err.println("Too few arguments for train-testing (need in-path at least). Use -h for help menu");
			return;
		}
		File trainDir = new File(args[1]);
		boolean self = args.length == 2 || !new File(args[2]).isDirectory();
		File testDir = self ? trainDir : new File(args[2]);
		Tokenizer tokenizer = getTokenizer(args);
		Vocabulary vocabulary = getVocabulary(args);
		Counter counter = Counter.standard();
		Model model = getModel(args, counter);
		System.out.println("Retrieving files");
		List<File> files = Util.getFiles(trainDir);
		System.out.println(files.size() + " files found");
		Training.countAll(files, tokenizer, vocabulary, counter);
		Testing.test("model", testDir, vocabulary, tokenizer, counter, model, useCache(args), self, deep(args));
	}
	
	private static void predict(String[] args) {
		if (args.length < 4) {
			System.err.println("Too few arguments for predicting (need in-path, out-path and vocabulary-path at least). Use -h for help menu");
			return;
		}
		File inDir = new File(args[1]);
		try {
			Counter counter = CountsReader.readCounter(new File(args[2]));
			Vocabulary vocabulary = Vocabulary.fromFile(new File(args[3]));
			Tokenizer tokenizer = getTokenizer(args);
			Model model = getModel(args, counter);
			Testing.test("predict", inDir, vocabulary, tokenizer, counter, model, useCache(args), self(args), deep(args));
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	private static void trainPredict(String[] args) {
		if (args.length < 2) {
			System.err.println("Too few arguments for train-predicting (need in-path at least). Use -h for help menu");
			return;
		}
		File trainDir = new File(args[1]);
		boolean self = args.length == 2 || !new File(args[2]).isDirectory();
		File testDir = self ? trainDir : new File(args[2]);
		Tokenizer tokenizer = getTokenizer(args);
		Vocabulary vocabulary = getVocabulary(args);
		Counter counter = Counter.standard();
		Model model = getModel(args, counter);
		System.out.println("Retrieving files");
		List<File> files = Util.getFiles(trainDir);
		System.out.println(files.size() + " files found");
		Training.countAll(files, tokenizer, vocabulary, counter);
		Testing.test("predict", testDir, vocabulary, tokenizer, counter, model, useCache(args), self, deep(args));
	}

	private static boolean isSet(String[] args, String arg) {
		for (int i = 0; i < args.length; i++) {
			String a = args[i];
			if (a.matches(arg)) return true;
		}
		return false;
	}

	private static String getArg(String[] args, String arg) {
		for (int i = 1; i < args.length; i++) {
			String a = args[i];
			if (a.contains("=") && a.substring(0, a.indexOf("=")).matches(arg)) {
				return a.substring(a.indexOf("=") + 1);
			}
			else if (a.matches(arg)) return "";
		}
		return null;
	}

	private static Tokenizer getTokenizer(String[] args) {
		String language = getArg(args, "(-l|--language)");
		Tokenizer tokenizer;
		if (language == null) tokenizer = Tokenizer.standard();
		else if (language.toLowerCase().equals("java")) tokenizer = new JavaTokenizer();
		else if (language.toLowerCase().equals("tokens")) tokenizer = new LineTokenizer();
		else if (language.toLowerCase().equals("blank")) tokenizer = new BlankTokenizer();
		else tokenizer = Tokenizer.standard();
		return tokenizer;
	}

	private static Vocabulary getVocabulary(String[] args) {
		String dir = getArg(args, "(-v|--vocabulary)");
		if (dir == null || dir.isEmpty()) return Vocabulary.empty();
		else return Vocabulary.fromFile(new File(dir));
	}

	private static Model getModel(String[] args, Counter counter) {
		String model = getArg(args, "(-m|--model)");
		Model m;
		if (model == null) m = Model.standard(counter);
		else if (model.toLowerCase().equals("jm")) m = new JMModel(counter);
		else if (model.toLowerCase().equals("wb")) m = new WBModel(counter);
		else if (model.toLowerCase().equals("ad")) m = new AbsDiscModel(counter);
		else if (model.toLowerCase().equals("adm")) m = new AbsDiscModModel(counter);
		else m = Model.standard(counter);
		return m;
	}

	private static int useCache(String[] args) {
		String value = getArg(args, "(-c|--cache)");
		if (value == null) return -1;
		else {
			if (value.isEmpty()) return CacheModel.DEFAULT_CAPACITY;
			else return Integer.parseInt(value);
		}
	}

	private static boolean self(String[] args) {
		return isSet(args, "(-s|--self)");
	}
	
	private static boolean deep(String[] args) {
		return isSet(args, "(-d|--deep)");
	}
}
