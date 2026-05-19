package me.eldodebug.flax.management.event;

import java.util.Iterator;

public class ArrayHelper<T> implements Iterable<T> {

	private T[] elements;

	public ArrayHelper(final T[] array) {
		this.elements = array;
	}

	@SuppressWarnings("unchecked")
	public ArrayHelper() {
		this.elements = (T[]) new Object[0];
	}

	@SuppressWarnings("unchecked")
	public void add(final T value) {
		if (value == null) {
			return;
		}

		Object[] array = new Object[size() + 1];
		for (int index = 0; index < array.length; index++) {
			array[index] = index < size() ? get(index) : value;
		}
		set((T[]) array);
	}

	@SuppressWarnings("unchecked")
	public boolean contains(final T value) {
		for (Object entry : array()) {
			if (entry.equals(value)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void remove(final T value) {
		if (!contains(value)) {
			return;
		}

		Object[] array = new Object[size() - 1];
		boolean skipped = false;
		for (int index = 0; index < size(); index++) {
			if (!skipped && get(index).equals(value)) {
				skipped = true;
			} else {
				array[skipped ? index - 1 : index] = get(index);
			}
		}
		set((T[]) array);
	}

	public T[] array() {
		return elements;
	}

	public int size() {
		return array().length;
	}

	public void set(final T[] array) {
		this.elements = array;
	}

	public T get(final int index) {
		return array()[index];
	}

	@SuppressWarnings("unchecked")
	public void clear() {
		elements = (T[]) new Object[0];
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<>() {
			private int index;

			@Override
			public boolean hasNext() {
				return index < size() && get(index) != null;
			}

			@Override
			public T next() {
				return get(index++);
			}

			@Override
			public void remove() {
				ArrayHelper.this.remove(get(index));
			}
		};
	}
}
