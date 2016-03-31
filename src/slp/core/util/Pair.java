package slp.core.util;

public class Pair<T, V> {
	public final T t;
	public final V v;
	
	public Pair(T t, V v) {
		this.t = t;
		this.v = v;
	}
	
	public static <A, B> Pair<A, B> of(A a, B b) {
		return new Pair<A, B>(a, b);
	}
}
