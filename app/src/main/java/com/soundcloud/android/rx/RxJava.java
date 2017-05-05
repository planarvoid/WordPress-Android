package com.soundcloud.android.rx;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * <p>Utility class to handle interoperability between RxJava 1 and RxJava 2.</p>
 * <p>This class exists ONLY for the sake of migrating V1 building blocks to V2.</p>
 * <p>Please REMOVE once the job is done.</p>
 */
public class RxJava {

    private RxJava() {}

    public static <T> rx.Observable<T> toV1Observable(Observable<T> sourceObservable) {
        return RxJavaInterop.toV1Observable(sourceObservable, BackpressureStrategy.ERROR);
    }

    public static <T> rx.Observable<T> toV1Observable(Single<T> sourceObservable) {
        return RxJavaInterop.toV1Single(sourceObservable).toObservable();
    }

    public static <T> rx.Observable<T> toV1Observable(Maybe<T> sourceObservable) {
        return RxJavaInterop.toV1Single(sourceObservable).toObservable();
    }

    public static <T> rx.Observable<T> toV1Observable(Flowable<T> sourceObservable) {
        return RxJavaInterop.toV1Observable(sourceObservable);
    }

    public static <T> Observable<T> toV2Observable(rx.Observable<T> sourceObservable) {
        return RxJavaInterop.toV2Observable(sourceObservable);
    }
}
