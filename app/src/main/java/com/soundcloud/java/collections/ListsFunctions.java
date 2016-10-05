package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Function;

public class ListsFunctions {
    public static <S, T extends S> Function<S, T> cast(final Class<T> clazz) {
        return new Function<S, T>() {
            @Override
            public T apply(S input) {
                return clazz.cast(input);
            }
        };
    }
}
