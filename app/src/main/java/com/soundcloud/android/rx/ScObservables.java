package com.soundcloud.android.rx;

import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import java.util.Arrays;
import java.util.List;

public class ScObservables {

    public static final Observable EMPTY = Observable.empty();

    public static <T> Observable<Observable<T>> pendingObservables(List<Observable<Observable<T>>> observables) {

        return Observable.from(observables)
                .map(new Func1<Observable<Observable<T>>, Observable<T>>() {
                    @Override
                    public Observable<T> call(Observable<Observable<T>> observableObservable) {
                        Log.d("Reducing " + observableObservable);
                        return observableObservable.last();
                    }
                })
                .filter(new Func1<Observable<T>, Boolean>() {
                    @Override
                    public Boolean call(Observable<T> observable) {
                        Log.d("filter: (" + observable + ")");
                        return ScObservables.EMPTY != observable;
                    }
                });
    }

    public static <T> Observable<Observable<T>> pendingObservables(Observable<Observable<T>>... observables) {
        return pendingObservables(Arrays.asList(observables));
    }

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

    //TODO: remove at some point, but useful for testing right now
    public static Observable<String> observableA() {
        return Observable.create(new Func1<Observer<String>, Subscription>() {
            @Override
            public Subscription call(Observer<String> observer) {
                observer.onNext("A");
                observer.onCompleted();
                return Subscriptions.empty();
            }
        });
    }

    //TODO: remove at some point, but useful for testing right now
    public static Observable<String> observableB() {
        return Observable.create(new Func1<Observer<String>, Subscription>() {
            @Override
            public Subscription call(Observer<String> observer) {
                observer.onNext("B");
                observer.onCompleted();
                return Subscriptions.empty();
            }
        });
    }

    //TODO: remove at some point, but useful for testing right now
    public static Observable<String> observableC() {
        return Observable.create(new Func1<Observer<String>, Subscription>() {
            @Override
            public Subscription call(Observer<String> observer) {
                observer.onNext("C");
                observer.onCompleted();
                return Subscriptions.empty();
            }
        });
    }
}
