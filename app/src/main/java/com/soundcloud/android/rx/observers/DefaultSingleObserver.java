package com.soundcloud.android.rx.observers;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.observers.ResourceSingleObserver;

import android.support.annotation.CallSuper;

/**
 * Default {@link ResourceObserver} base class to be used whenever you want default error handling
 */
public class DefaultSingleObserver<T> extends ResourceSingleObserver<T> {

    private final ErrorReporter errorReporter = new ErrorReporter();

    @Override
    protected void onStart() {
        errorReporter.handleOnStart();
    }

    @CallSuper
    @Override
    public void onSuccess(@NonNull T t) {
        errorReporter.handleOnComplete();
    }

    @Override
    public void onError(@NonNull Throwable throwable) {
        errorReporter.handleOnError(throwable);
    }

}
