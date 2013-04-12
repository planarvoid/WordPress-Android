package com.soundcloud.android.rx;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

public class ScObservables {

    public static final Observable EMPTY = Observable.empty();

    public static <T> Observable<Observable<T>> pending(final Observable<T> observable) {
        return Observable.create(new Func1<Observer<Observable<T>>, Subscription>() {
            @Override
            public Subscription call(Observer<Observable<T>> observer) {
                observer.onNext(observable);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        });
    }

}
