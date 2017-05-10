package slp.core.counting.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import slp.core.counting.Counter;

public class CounterIO {

	public static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

	public static Counter readCounter(File file) {
		try {
			FileInputStream ois = new FileInputStream(file);
			FSTObjectInput in = conf.getObjectInput(ois);
		    Counter result = (Counter) in.readObject(Counter.class);
		    ois.close();
		    return result;
		} catch (Exception e) {
			System.err.println("Error while writing counter to file " + file);
			e.printStackTrace();
			return null;
		}
	}

	public static void writeCounter(Counter counter, File file) {
		try {
			FileOutputStream outStream = new FileOutputStream(file);
			FSTObjectOutput out = conf.getObjectOutput(outStream);
			out.writeObject(counter, Counter.class);
			out.flush();
			outStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
