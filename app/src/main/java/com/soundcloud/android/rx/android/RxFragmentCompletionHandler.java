package com.soundcloud.android.rx.android;

import rx.util.functions.Action0;

import android.support.v4.app.Fragment;

public abstract class RxFragmentCompletionHandler<T extends Fragment> extends RxFragmentHandler<T> implements Action0, RxFragmentOnCompletedMethods<T> {

    public RxFragmentCompletionHandler(T fragment) {
        super(fragment);
    }

    @Override
    public final void call() {
        T fragment = getFragmentForCallback();
        if (fragment != null) {
            onCompleted(fragment);
        }
    }
}
