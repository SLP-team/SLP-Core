package slp.core.counting.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import slp.core.counting.Counter;
import slp.core.counting.Vocabulary;

public class CountsWriter {

	public static void write(Vocabulary vocabulary, Counter counter, File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		ObjectOutputStream o = new ObjectOutputStream(out);
		o.writeObject(vocabulary);
		o.writeObject(counter);
		o.close();
	}
	
	public static void writeCounter(Counter counter, File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		ObjectOutputStream o = new ObjectOutputStream(out);
		o.writeObject(counter);
		o.close();
	}
}
