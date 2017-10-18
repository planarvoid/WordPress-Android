package com.soundcloud.android.rx.observers;

import io.reactivex.observers.ResourceObserver;

import android.support.annotation.CallSuper;

/**
 * Default {@link ResourceObserver} base class to be used whenever you want default error handling
 */
public class DefaultObserver<T> extends ResourceObserver<T> {

    private final ErrorReporter errorReporter = new ErrorReporter();

    @Override
    protected void onStart() {
        errorReporter.handleOnStart();
    }

    @Override
    public void onNext(T object) {
        // no-op by default.
    }

    @Override
    public void onComplete() {
        errorReporter.handleOnComplete();
    }

    @Override
    @CallSuper
    public void onError(Throwable throwable) {
        errorReporter.handleOnError(throwable);
    }
}
