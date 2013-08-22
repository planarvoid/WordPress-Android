package com.soundcloud.android.rx;

import rx.Observer;

public class RxUtils {

    public static <T> void emitIterable(Observer<T> observer, Iterable<T> iterable) {
        for (T item : iterable){
            observer.onNext(item);
        }
    }
}
