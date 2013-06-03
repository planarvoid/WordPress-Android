package com.soundcloud.android.rx.android;

import rx.util.functions.Action0;

import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;

public abstract class RxFragmentCompletionHandler<T extends Fragment> implements Action0 {

    private final WeakReference<T> mFragmentRef;
    private boolean mRequireActivity = true;

    public RxFragmentCompletionHandler(T fragment) {
        mFragmentRef = new WeakReference<T>(fragment);
    }

    @SuppressWarnings("unused")
    public void setRequireActivity(boolean requireActivity) {
        mRequireActivity = requireActivity;
    }

    @Override
    public final void call() {
        T fragment = mFragmentRef.get();
        if (fragment != null && (!mRequireActivity || fragment.isAdded())) {
            onCompleted(fragment);
        }
    }

    protected abstract void onCompleted(T fragment);
}
