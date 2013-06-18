package com.soundcloud.android.rx.android;

import rx.util.functions.Action1;

import android.support.v4.app.Fragment;

public abstract class RxFragmentErrorHandler<T extends Fragment> extends RxFragmentHandler<T> implements Action1<Exception>, RxFragmentOnErrorMethods<T> {

    public RxFragmentErrorHandler(T fragment) {
        super(fragment);
    }

    @Override
    public final void call(Exception error) {
        T fragment = getFragmentForCallback();
        if (fragment != null) {
            onError(fragment, error);
        }
    }
}
