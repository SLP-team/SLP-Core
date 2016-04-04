package slp.core.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

public class Buffer<T> implements Function<T, Stream<T>> {
	private int capacity;
	private T[] queue;
	private int queueStart;
	private int queueEnd;

	public Buffer() {
		clean();
	}

	public Buffer(int capacity) {
		clean(capacity);
	}

	public void clean() {
		clean(Configuration.order());
	}

	public void clean(int capacity) {
		this.capacity = capacity;
		this.queue = null;
		this.queueStart = 0;
		this.queueEnd = 0;
	}

	public static <X> Buffer<X> of(Class<X> type) {
		return new Buffer<X>();
	}

	private void add(T t) {
		int length = this.capacity + 1;

		int position = this.queueEnd;
		int nextEnd = (this.queueEnd + 1) % length;

		if (nextEnd == this.queueStart) { // we're full, get rid of one element
			this.queueStart = (this.queueStart + 1) % length;
		}
		this.queueEnd = nextEnd;
		this.queue[position] = t;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Stream<T> apply(T t) {
		if (this.queue == null) {
			this.queue = (T[]) Array.newInstance(t.getClass(), this.capacity + 1);
		}
		int length = this.capacity + 1;

		add(t);

		if (this.queueStart < this.queueEnd) {
			T[] copyOfRange = Arrays.copyOfRange(this.queue, this.queueStart, this.queueEnd);
			return Arrays.stream(copyOfRange);
		} else {
			int num = (this.queueEnd - this.queueStart + length) % length;
			T[] result = (T[]) Array.newInstance(t.getClass(), num);

			System.arraycopy(this.queue, this.queueStart, result, 0, length - this.queueStart);
			System.arraycopy(this.queue, 0, result, length - this.queueStart, this.queueEnd);
			return Arrays.stream(result);
		}
	}
}
