package com.soundcloud.android.rx.observers;

import rx.Subscription;

public class RxObserverHelper {
    public static Subscription fireAndForget(rx.Observable<?> observable) {
        return observable.subscribe(DefaultObserver.NOOP_OBSERVER);
    }
}
