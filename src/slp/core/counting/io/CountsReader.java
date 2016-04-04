package slp.core.counting.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;
import slp.core.util.Pair;

public class CountsReader {

	public static Pair<Vocabulary, Counter> read(File file) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		Vocabulary vocabulary = (Vocabulary) ois.readObject();
		Counter counter = (Counter) ois.readObject();
		ois.close();
		return Pair.of(vocabulary, counter);
	}
	
	public static Vocabulary readVocabulary(File file) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		Vocabulary vocabulary = (Vocabulary) ois.readObject();
		ois.close();
		return vocabulary;
	}

	public static Counter readCounter(File file) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		Counter counter = (Counter) ois.readObject();
		ois.close();
		return counter;
	}
}
