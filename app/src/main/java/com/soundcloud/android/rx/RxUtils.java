package com.soundcloud.android.rx;

import rx.Observer;

public final class RxUtils {

    public static <T> void emitIterable(Observer<? super T> observer, Iterable<T> iterable) {
        for (T item : iterable){
            observer.onNext(item);
        }
    }

    private RxUtils() {}
}
