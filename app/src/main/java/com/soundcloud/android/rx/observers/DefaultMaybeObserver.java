package com.soundcloud.android.rx.observers;

import io.reactivex.observers.ResourceMaybeObserver;
import io.reactivex.observers.ResourceObserver;

/**
 * Default {@link ResourceObserver} base class to be used whenever you want default error handling
 */
public class DefaultMaybeObserver<T> extends ResourceMaybeObserver<T> {

    private final ErrorReporter errorReporter = new ErrorReporter();

    @Override
    protected void onStart() {
        errorReporter.handleOnStart();
    }

    @Override
    public void onSuccess(T object) {
        // no-op by default.
    }

    @Override
    public void onComplete() {
        errorReporter.handleOnComplete();
    }

    @Override
    public void onError(Throwable throwable) {
        errorReporter.handleOnError(throwable);
    }
}
