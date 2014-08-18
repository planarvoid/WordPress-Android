package com.soundcloud.android.rx.observers;

import com.soundcloud.android.utils.CallsiteToken;
import com.soundcloud.android.utils.ErrorUtils;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * Default subscriber base class to be used whenever you want default error handling
 * (cf. {@link com.soundcloud.android.utils.ErrorUtils#handleThrowable(Throwable, com.soundcloud.android.utils.CallsiteToken)}
 */
public class DefaultSubscriber<T> extends Subscriber<T> {

    private final CallsiteToken callsiteToken = CallsiteToken.build();

    public static <T> Subscription fireAndForget(Observable<T> observable) {
        return observable.subscribe(new DefaultSubscriber<T>());
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
        ErrorUtils.handleThrowable(e, callsiteToken);
    }

    @Override
    public void onNext(T args) {
    }
}
