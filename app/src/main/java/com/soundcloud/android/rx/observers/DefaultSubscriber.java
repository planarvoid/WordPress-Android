package com.soundcloud.android.rx.observers;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * <p>Default subscriber base class to be used whenever you want default error handling
 * (cf. {@link com.soundcloud.android.utils.ErrorUtils#handleThrowable(Throwable, com.soundcloud.android.utils.CallsiteToken)}</p>
 *
 * <p>This class has been deprecated in favor of {@link DefaultObserver} and for the sake of migrating from RxJava 1 to
 * RxJava 2.</p>
 */
@Deprecated
public class DefaultSubscriber<T> extends Subscriber<T> {

    private final ErrorReporter errorReporter = new ErrorReporter();

    public static <T> Subscription fireAndForget(Observable<T> observable) {
        return observable.subscribe(new DefaultSubscriber<>());
    }

    public DefaultSubscriber(){
    }

    @Override
    public void onStart() {
        errorReporter.handleOnStart();
    }

    @Override
    public void onNext(T args) {
        // no-op by default.
    }

    @Override
    public void onCompleted() {
        errorReporter.handleOnComplete();
    }

    @Override
    public void onError(Throwable throwable) {
        errorReporter.handleOnError(throwable);
    }
}
