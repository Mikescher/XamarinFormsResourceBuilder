package com.mikescher.xamarinforms.resourcebuilder.values;

import java.util.Objects;

public class Tuple2<T1, T2> {
	public final T1 Item1;
	public final T2 Item2;

	private Tuple2(T1 i1, T2 i2) {
		this.Item1 = i1;
		this.Item2 = i2;
	}

	public static <T1, T2> Tuple2<T1, T2> Create(T1 i1, T2 i2) {
		return new Tuple2<>(i1, i2);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		
		if (o == this) return true;

		if (! (o instanceof Tuple2<?, ?>)) return false;
		
		Tuple2<?, ?> other = (Tuple2<?, ?>) o;
		
		if (other.Item1 == null && Item1 != null) return false;
		if (other.Item1 == null) return false;
		if (! other.Item1.equals(Item1)) return false;

		if (other.Item2 == null && Item2 != null) return false;
		if (other.Item2 == null) return false;

		return other.Item2.equals(Item2);
	}

	@Override
	public int hashCode() {
		return Objects.hash(Item1, Item2);
	}

	@Override
	@SuppressWarnings("nls")
	public String toString() {
		String s1 = "NULL";
		if (Item1 != null) s1 = Item1.toString();

		String s2 = "NULL";
		if (Item2 != null) s2 = Item2.toString();

		return "<" + s1 + ", " + s2 + ">";
	}

}