package com.soundcloud.android.rx;

import rx.Observable;

import java.util.concurrent.TimeUnit;

public class OperationsInstrumentation {

    public static <T> Observable.Transformer<T, T> timeout() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> operation) {
                return operation.timeout(5, TimeUnit.SECONDS, Observable.<T>error(new UnresponsiveAppException()));
            }
        };
    }

    public static class UnresponsiveAppException extends RuntimeException {
    }
}
