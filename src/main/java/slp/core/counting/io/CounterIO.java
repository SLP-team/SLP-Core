package slp.core.counting.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

import slp.core.counting.Counter;

public class CounterIO {

	private static final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("river");
	private static final MarshallingConfiguration configuration = new MarshallingConfiguration();
    static {
    	configuration.setVersion(3);
    }
    
	public static Counter readCounter(File file) {
        try (FileInputStream is = new FileInputStream(file)) {
        	final Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(configuration);
            unmarshaller.start(Marshalling.createByteInput(is));
            Counter counter = (Counter) unmarshaller.readObject();
            unmarshaller.finish();
            is.close();
            return counter;
        } catch (IOException | ClassNotFoundException e) {
            System.err.print("Un-marshalling failed: ");
            e.printStackTrace();
        }
		return null;
	}

	public static void writeCounter(Counter counter, File file) {
		try (FileOutputStream os = new FileOutputStream(file)) {
        	final Marshaller marshaller = marshallerFactory.createMarshaller(configuration);
            marshaller.start(Marshalling.createByteOutput(os));
            marshaller.writeObject(counter);
            marshaller.finish();
            os.close();
        } catch (IOException e) {
            System.err.print("Marshalling failed: ");
            e.printStackTrace();
        }
	}
}
