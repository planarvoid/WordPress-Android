package com.soundcloud.android.rx.observers;

import com.soundcloud.android.utils.ErrorUtils;
import rx.Observer;

/**
 * Adapter class for the RX Observer. Use this when you do not have to override all methods in an observer.
 */
//TODO: replace with RxFragmentObserver
public abstract class ScObserver<T> implements Observer<T> {
    @Override
    public void onCompleted() {}

    @Override
    public void onError(Throwable e) {
        ErrorUtils.handleThrowable(e);
    }

    @Override
    public void onNext(T args) {}
}
