package slp.core.util;

public class Temp {

	public static void main(String[] args) {
		int[] arr = { Integer.MIN_VALUE, 2, Integer.MIN_VALUE, Integer.MIN_VALUE, 6, 8, Integer.MIN_VALUE };
		int ix = binSearch(arr, 5);
		if (ix < 0) ix = -ix - 1;
		System.out.println(ix);
	}
	
	private static int binSearch(int[] arr, int key) {
//		if (Math.random() < 0.01 && arr.length > 10) {
//			System.out.println(arr.length + "\t" + (double) Arrays.stream(arr).filter(x -> x >= 0).count() / arr.length);
//		}
		int low = 0;
		int high = arr.length - 1;
		OUTER:
		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midVal = arr[mid];
			int x = 1;
			boolean lb = true;
			boolean rb = true;
			while (midVal < 0) {
				if (!rb || mid + x > high) rb = false;
				if (!lb || mid - x < low) lb = false;
				if (!lb && !rb) break OUTER;
				else if (rb && arr[mid + x] != Integer.MIN_VALUE) {
					midVal = arr[mid + x];
					mid = mid + x;
				}
				else if (lb && arr[mid - x] != Integer.MIN_VALUE) {
					midVal = arr[mid - x];
					mid = mid - x;
				}
				x++;
			}
			if (midVal < key) low = mid + 1;
			else if (midVal > key) high = mid - 1;
			else return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

}
