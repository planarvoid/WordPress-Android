package com.soundcloud.android.rx;

import com.soundcloud.android.rx.observers.DefaultObserver;
import rx.Observer;
import rx.Subscription;

public class RxUtils {

    public static <T> void emitIterable(Observer<? super T> observer, Iterable<T> iterable) {
        for (T item : iterable){
            observer.onNext(item);
        }
    }

    public static Subscription fireAndForget(rx.Observable<?> observable) {
        return observable.subscribe(DefaultObserver.NOOP_OBSERVER);
    }
}
