package com.soundcloud.android.rx;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class TestObservables {

    public static <T> Observable<T> errorThrowingObservable() {
        return Observable.create(new Observable.OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                throw new RuntimeException();
            }
        });
    }

    public static Observable<Boolean> booleanObservable(final boolean value) {
        return Observable.create(new Observable.OnSubscribeFunc<Boolean>() {
            @Override
            public Subscription onSubscribe(Observer<? super Boolean> observer) {
                observer.onNext(value);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        });
    }
}
