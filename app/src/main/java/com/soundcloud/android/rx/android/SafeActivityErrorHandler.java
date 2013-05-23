package com.soundcloud.android.rx.android;

import rx.util.functions.Action1;

import android.app.Activity;

import java.lang.ref.WeakReference;

public abstract class SafeActivityErrorHandler implements Action1<Exception> {

    private final WeakReference<Activity> mActivityRef;

    public SafeActivityErrorHandler(Activity context) {
        mActivityRef = new WeakReference<Activity>(context);
    }

    @Override
    public final void call(Exception error) {
        Activity activity = mActivityRef.get();
        if (activity != null && !activity.isFinishing()) {
            handleError(activity, error);
        }
    }

    protected abstract void handleError(Activity activity, Exception error);
}
