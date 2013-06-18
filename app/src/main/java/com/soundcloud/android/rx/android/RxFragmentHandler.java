package com.soundcloud.android.rx.android;

import org.jetbrains.annotations.Nullable;

import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;

public abstract class RxFragmentHandler<T extends Fragment> {

    private final WeakReference<T> mFragmentRef;
    private boolean mRequireActivity = true;

    protected RxFragmentHandler(T fragment) {
        this.mFragmentRef = new WeakReference<T>(fragment);
    }

    @Nullable
    protected T getFragmentForCallback() {
        T fragment = mFragmentRef.get();
        if (fragment != null && (!isRequireActivity() || fragment.isAdded())) {
            return fragment;
        }
        return null;
    }

    @SuppressWarnings("unused")
    public void setRequireActivity(boolean requireActivity) {
        mRequireActivity = requireActivity;
    }

    public boolean isRequireActivity() {
        return mRequireActivity;
    }
}
