package core.util;

public class Pair<T, V> {
	public final T left;
	public final V right;
	
	public Pair(T t, V v) {
		this.left = t;
		this.right = v;
	}
	
	public static <A, B> Pair<A, B> of(A a, B b) {
		return new Pair<A, B>(a, b);
	}
	
	@Override
	public String toString() {
		return this.left + " :: " + this.right;
	}
}
