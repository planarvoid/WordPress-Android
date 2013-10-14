package com.soundcloud.android.rx;

import rx.Observable;
import rx.Observer;
import rx.Subscription;

public class TestObservables {

    public static <T> Observable<T> errorThrowingObservable() {
        return Observable.create(new Observable.OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                throw new RuntimeException();
            }
        });
    }
}
