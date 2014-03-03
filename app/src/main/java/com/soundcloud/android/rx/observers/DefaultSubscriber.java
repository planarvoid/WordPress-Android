package com.soundcloud.android.rx.observers;

import com.soundcloud.android.utils.ErrorUtils;
import rx.Subscriber;
import rx.Subscription;

/**
 * Default subscriber base class to be used whenever you want default error handling
 * (cf. {@link com.soundcloud.android.utils.ErrorUtils#handleThrowable(Throwable)}
 */
public abstract class DefaultSubscriber<T> extends Subscriber<T> {

    /**
     * For fire and forget style subscriptions. Do not use this directly, use {@link DefaultSubscriber#fireAndForget(rx.Observable)})}
     * instead.
     */
    /* package */ static final DefaultSubscriber<Object> NOOP_SUBSCRIBER = new DefaultSubscriber<Object>() {
    };

    public static Subscription fireAndForget(rx.Observable<?> observable) {
        return observable.subscribe(NOOP_SUBSCRIBER);
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
        ErrorUtils.handleThrowable(e);
    }

    @Override
    public void onNext(T args) {
    }
}
