package com.soundcloud.android.rx.android;

import rx.util.functions.Action1;

import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;

public abstract class RxFragmentErrorHandler<T extends Fragment> implements Action1<Exception> {

    private final WeakReference<T> mFragmentRef;

    public RxFragmentErrorHandler(T fragment) {
        mFragmentRef = new WeakReference<T>(fragment);
    }

    @Override
    public final void call(Exception error) {
        T fragment = mFragmentRef.get();
        if (fragment != null && fragment.isAdded()) {
            onError(fragment, error);
        }
    }

    protected abstract void onError(T fragment, Exception error);
}
