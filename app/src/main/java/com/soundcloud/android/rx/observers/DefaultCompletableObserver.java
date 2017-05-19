package com.soundcloud.android.rx.observers;

import io.reactivex.observers.ResourceCompletableObserver;
import io.reactivex.observers.ResourceObserver;

/**
 * Default {@link ResourceObserver} base class to be used whenever you want default error handling
 */
public class DefaultCompletableObserver extends ResourceCompletableObserver {

    private final ErrorReporter errorReporter = new ErrorReporter();

    @Override
    protected void onStart() {
        errorReporter.handleOnStart();
    }

    @Override
    public void onComplete() {
        // no-op by default.
    }

    @Override
    public void onError(Throwable throwable) {
        errorReporter.handleOnError(throwable);
    }
}
