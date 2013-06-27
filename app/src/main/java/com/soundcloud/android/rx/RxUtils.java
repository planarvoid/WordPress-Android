package com.soundcloud.android.rx;

import rx.Observer;

import java.util.List;

public class RxUtils {

    public static <T> void emitList(Observer<T> observer, List<T> list){
        for (T item : list){
            observer.onNext(item);
        }
    }

}
