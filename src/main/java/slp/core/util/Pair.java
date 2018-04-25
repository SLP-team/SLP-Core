package slp.core.util;

public class Pair<T, V> {
	public T left;
	public V right;
	
	public Pair(T t, V v) {
		this.left = t;
		this.right = v;
	}
	
	public static <A, B> Pair<A, B> of(A a, B b) {
		return new Pair<A, B>(a, b);
	}

	public T left() {
		return this.left;
	}
	
	public V right() {
		return this.right;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (!(other instanceof Pair)) return false;
		@SuppressWarnings("rawtypes")
		Pair asPair = (Pair) other;
		return asPair.left.equals(this.left) && asPair.right.equals(this.right);
	}
	
	@Override
	public int hashCode() {
		return this.left.hashCode() + this.right.hashCode();
	}
	
	@Override
	public String toString() {
		return this.left + " :: " + this.right;
	}
}
