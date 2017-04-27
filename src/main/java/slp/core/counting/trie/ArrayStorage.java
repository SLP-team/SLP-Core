package slp.core.counting.trie;

import java.util.List;

/*
 * Array storage
 */
public class ArrayStorage {
	public static boolean checkExactSequence(List<Integer> indices, int index, int[] successor) {
		boolean valid = successor.length == indices.size() - index;
		if (valid) {
			for (int i = 1; i < successor.length; i++) {
				if (indices.get(index + i) != successor[i]) {
					valid = false;
					break;
				}
			}
		}
		return valid;
	}

	public static boolean checkPartialSequence(List<Integer> indices, int index, int[] successor) {
		boolean valid = successor.length >= indices.size() - index;
		if (valid) {
			for (int i = 1; i < indices.size() - index; i++) {
				if (indices.get(index + i) != successor[i]) {
					valid = false;
					break;
				}
			}
		}
		return valid;
	}
}