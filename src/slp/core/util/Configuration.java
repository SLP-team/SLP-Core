package slp.core.util;

public class Configuration {

	private static int ORDER = 7;
	private static int UNK_CUTOFF = 0;

	public static int order() {
		return ORDER;
	}
	
	public static void setOrder(int order) {
		ORDER = order;
	}

	public static int unkCutof() {
		return UNK_CUTOFF;
	}
	
	public static void setUNKCutoff(int cutoff) {
		UNK_CUTOFF = cutoff;
	}
}
