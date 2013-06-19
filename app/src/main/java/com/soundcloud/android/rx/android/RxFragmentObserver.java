package com.soundcloud.android.rx.android;

import rx.Observer;

import android.support.v4.app.Fragment;

public class RxFragmentObserver<T extends Fragment, R> extends RxFragmentHandler<T>
        implements Observer<R>, RxFragmentOnNextMethods<T, R>, RxFragmentOnCompletedMethods<T>, RxFragmentOnErrorMethods<T> {


    public RxFragmentObserver(T fragment) {
        super(fragment);
    }

    @Override
    public void onCompleted() {
        T fragment = getFragmentForCallback();
        if (fragment != null) onCompleted(fragment);
    }

    @Override
    public void onError(Exception e) {
        T fragment = getFragmentForCallback();
        if (fragment != null) onError(fragment, e);
    }

    @Override
    public void onNext(R element) {
        T fragment = getFragmentForCallback();
        if (fragment != null) onNext(fragment, element);
    }

    @Override
    public void onCompleted(T fragment) {
    }

    @Override
    public void onError(T fragment, Exception error) {
    }

    @Override
    public void onNext(T fragment, R element) {
    }
}
