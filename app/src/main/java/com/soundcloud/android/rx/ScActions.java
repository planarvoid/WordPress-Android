package com.soundcloud.android.rx;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Action1;

public class ScActions {

    public static <T> Action1<Observable<T>> pendingAction(final Observer<T> observer) {
        return new Action1<Observable<T>>() {
            @Override
            public void call(Observable<T> observable) {
                observable.subscribe(observer);
            }
        };
    }

}
