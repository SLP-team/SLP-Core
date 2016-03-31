package slp.core.counting;

public interface CounterProps<T extends Counter> {
	
	public abstract T getCounter();
	
	public abstract int getOverallCount();	
	public abstract int getOverallDistinct();

	public abstract int[][] getFullCounts(Integer[] indices);
	public abstract int[][] getFullCounts(Integer[] indices, int startIndex);
	public abstract int[][] getFullCounts(Integer[] indices, int startIndex, int endIndex);

	public abstract int[] getShortCounts(Integer[] indices);
	public abstract int[] getShortCounts(Integer[] indices, int startIndex);
	public abstract int[] getShortCounts(Integer[] indices, int startIndex, int endIndex);
	
	public abstract int[] getDistinctCounts(int range, Integer[] indices);
	public abstract int[] getDistinctCounts(int range, Integer[] indices, int startIndex);
	public abstract int[] getDistinctCounts(int range, Integer[] indices, int startIndex, int endIndex);
}
