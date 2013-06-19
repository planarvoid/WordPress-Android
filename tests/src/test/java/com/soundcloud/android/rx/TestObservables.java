package com.soundcloud.android.rx;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public class TestObservables {

    public static <T> Observable<T> errorThrowingObservable(final Exception error) {
        return Observable.create(new Func1<Observer<T>, Subscription>() {
            @Override
            public Subscription call(Observer<T> observer) {
                throw new RuntimeException();
            }
        });
    }

}
