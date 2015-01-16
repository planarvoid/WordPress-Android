package com.soundcloud.android.testsupport;

import dagger.Lazy;

public class InjectionSupport {

    public static <T> Lazy<T> lazyOf(final T value) {
        return new Lazy<T>() {
            @Override
            public T get() {
                return value;
            }
        };
    }

}
