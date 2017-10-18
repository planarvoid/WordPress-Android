package com.soundcloud.android.rx.observers;

import io.reactivex.observers.ResourceCompletableObserver;

import android.support.annotation.CallSuper;

public class DefaultCompletableObserver extends ResourceCompletableObserver {

    private final ErrorReporter errorReporter = new ErrorReporter();

    @Override
    protected void onStart() {
        errorReporter.handleOnStart();
    }

    @Override
    public void onComplete() {
        errorReporter.handleOnComplete();
    }

    @Override
    @CallSuper
    public void onError(Throwable e) {
        errorReporter.handleOnError(e);
    }
}
