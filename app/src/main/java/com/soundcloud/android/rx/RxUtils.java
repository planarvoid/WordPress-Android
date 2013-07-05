package com.soundcloud.android.rx;

import rx.Observer;

import java.util.Collection;

public class RxUtils {

    public static <T> void emitCollection(Observer<T> observer, Collection<T> collection) {
        for (T item : collection){
            observer.onNext(item);
        }
    }

}
