package com.soundcloud.android.rx;

import com.soundcloud.android.rx.observers.DefaultObserver;

public class CrashOnTerminateSubscriber<T> extends DefaultObserver<T> {

    @Override
    public void onError(Throwable e) {
        super.onError(new IllegalStateException(e));
    }

    @Override
    public void onComplete() {
        throw new IllegalStateException("Subscription should not terminate");
    }
}
