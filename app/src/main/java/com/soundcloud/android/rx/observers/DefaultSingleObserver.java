package com.soundcloud.android.rx.observers;

import io.reactivex.observers.ResourceObserver;
import io.reactivex.observers.ResourceSingleObserver;

/**
 * Default {@link ResourceObserver} base class to be used whenever you want default error handling
 */
public class DefaultSingleObserver<T> extends ResourceSingleObserver<T> {

    private final ErrorReporter errorReporter = new ErrorReporter();

    @Override
    protected void onStart() {
        errorReporter.handleOnStart();
    }

    @Override
    public void onSuccess(T t) {
        // no-op by default.
    }

    @Override
    public void onError(Throwable throwable) {
        errorReporter.handleOnError(throwable);
    }
}
