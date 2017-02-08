package com.soundcloud.android.rx;

import com.soundcloud.android.rx.observers.DefaultSubscriber;

public class CrashOnTerminateSubscriber<T> extends DefaultSubscriber<T> {

    @Override
    public void onError(Throwable e) {
        super.onError(new IllegalStateException(e));
    }

    @Override
    public void onCompleted() {
        throw new IllegalStateException("Subscription should not terminate");
    }
}
