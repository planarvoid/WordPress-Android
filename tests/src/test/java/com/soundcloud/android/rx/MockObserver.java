package com.soundcloud.android.rx;

import rx.Observer;

public class MockObserver<T> implements Observer<T> {
    private boolean onNextCalled, onCompletedCalled, onErrorCalled;
    private Observer<T> target;

    public static <T> MockObserver<T> from(Observer<T> source) {
        return new MockObserver<T>(source);
    }

    public MockObserver(Observer<T> target) {
        this.target = target;
    }

    @Override
    public void onCompleted() {
        onCompletedCalled = true;
        target.onCompleted();
    }

    @Override
    public void onError(Exception e) {
        onErrorCalled = true;
        target.onError(e);
    }

    @Override
    public void onNext(T args) {
        onNextCalled = true;
        target.onNext(args);
    }

    public boolean isOnNextCalled() {
        return onNextCalled;
    }

    public boolean isOnCompletedCalled() {
        return onCompletedCalled;
    }

    public boolean isOnErrorCalled() {
        return onErrorCalled;
    }
}
