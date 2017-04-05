package core.counting.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import core.counting.Counter;

public class CountsWriter {

	public static void writeCounter(Counter counter, File file) {
		try {
			FileOutputStream out = new FileOutputStream(file);
			ObjectOutputStream o = new ObjectOutputStream(out);
			o.writeObject(counter);
			o.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
