package com.soundcloud.android.rx;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;

public final class RxUtils {

    public static final Func1<Boolean, Boolean> IS_TRUE = new Func1<Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean isEnabled) {
            return isEnabled;
        }
    };

    public static final Func1<Object, Void> TO_VOID = new Func1<Object, Void>() {
        @Override
        public Void call(Object ignore) {
            return null;
        }
    };

    public static <T> void emitIterable(Observer<? super T> observer, Iterable<T> iterable) {
        for (T item : iterable){
            observer.onNext(item);
        }
    }

    public static <T> Func1<Object, T> returning(final T obj) {
        return new Func1<Object, T>() {
            @Override
            public T call(Object o) {
                return obj;
            }
        };
    }

    public static <T> Func1<Object, Observable<T>> continueWith(final Observable<T> continuation) {
        return new Func1<Object, Observable<T>>() {
            @Override
            public Observable<T> call(Object o) {
                return continuation;
            }
        };
    }

    private RxUtils() {}
}
