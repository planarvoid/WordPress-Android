package com.soundcloud.android.rx.observers;

import io.reactivex.annotations.NonNull;
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
    public void onSuccess(@NonNull T t) {
        // no-op
    }

    @Override
    public void onError(@NonNull Throwable throwable) {
        errorReporter.handleOnError(throwable);
    }

    @Override
    public void onComplete() {
        errorReporter.handleOnComplete();
    }

}
