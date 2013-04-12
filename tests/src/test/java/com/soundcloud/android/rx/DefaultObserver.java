package com.soundcloud.android.rx;

import rx.Observer;

public class DefaultObserver<T> implements Observer<T> {
    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Exception e) {
    }

    @Override
    public void onNext(T args) {
    }
}
