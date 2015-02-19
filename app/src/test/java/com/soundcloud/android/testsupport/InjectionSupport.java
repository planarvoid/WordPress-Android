package com.soundcloud.android.testsupport;

import dagger.Lazy;

import javax.inject.Provider;

public class InjectionSupport {

    public static <T> Lazy<T> lazyOf(final T value) {
        return new Lazy<T>() {
            @Override
            public T get() {
                return value;
            }
        };
    }

    public static <T> Provider<T> providerOf(final T value) {
        return new Provider<T>() {
            @Override
            public T get() {
                return value;
            }
        };
    }

}
