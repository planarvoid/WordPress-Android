package com.soundcloud.java.collections;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.java.objects.MoreObjects;

public final class Pair<A, B> {

    private final A a;
    private final B b;

    /**
     * Creates a pair of two non-null objects.
     *
     * @param first  the first non-null element
     * @param second the second non-null element
     * @throws NullPointerException if either element is null
     */
    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }

    Pair(A a, B b) {
        checkNotNull(a);
        checkNotNull(b);
        this.a = a;
        this.b = b;
    }

    public A first() {
        return a;
    }

    public B second() {
        return b;
    }

    public boolean equals(Object o) {
        return o instanceof Pair
                && MoreObjects.equal(a, ((Pair) o).a)
                && MoreObjects.equal(b, ((Pair) o).b);
    }

    public int hashCode() {
        return a.hashCode() ^ b.hashCode();
    }

    public String toString() {
        return "(" + a + "=" + b + ")";
    }
}
