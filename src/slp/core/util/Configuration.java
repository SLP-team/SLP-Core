package slp.core.util;

public class Configuration {

	private static int ORDER = 6;
	private static int PREDICTION_CUTOFF = 10;
	private static int UNK_CUTOFF = 0;
	private static boolean CLOSED = false;

	public static int order() {
		return ORDER;
	}
	
	public static void setOrder(int order) {
		ORDER = order;
	}

	public static int predictionCutoff() {
		return PREDICTION_CUTOFF;
	}
	
	public static void setPredictionCutoff(int cutoff) {
		PREDICTION_CUTOFF = cutoff;
	}

	public static int unkCutof() {
		return UNK_CUTOFF;
	}
	
	public static void setUNKCutoff(int cutoff) {
		UNK_CUTOFF = cutoff;
	}
	
	public static void setClosed(boolean closed) {
		CLOSED = closed;
	}
	
	public static boolean closed() {
		return CLOSED;
	}
}
