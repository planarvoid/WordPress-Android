package com.soundcloud.android.utils;

public interface Function<T, R> {
    R apply(T var1);

    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        throw new RuntimeException("Stub!");
    }

    default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
        throw new RuntimeException("Stub!");
    }
}
