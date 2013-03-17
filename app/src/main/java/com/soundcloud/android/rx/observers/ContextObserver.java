package com.soundcloud.android.rx.observers;

import com.soundcloud.android.utils.Log;
import rx.Observer;

import android.os.Handler;
import android.os.Looper;

public class ContextObserver<T> implements Observer<T> {

    private Observer<T> mDelegate;
    private Handler mHandler;
    private volatile boolean mCompleted;

    public ContextObserver(Observer<T> delegate) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException(getClass().getSimpleName() + " must be instantiated on the main thread");
        }

        mDelegate = delegate;
        mHandler = new Handler();
    }

    @Override
    public void onCompleted() {
        mCompleted = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDelegate.onCompleted();
            }
        });
    }

    @Override
    public void onError(final Exception e) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDelegate.onError(e);
            }
        });
    }

    @Override
    public void onNext(final T args) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDelegate.onNext(args);
            }
        });
    }

    public boolean isCompleted() {
        return mCompleted;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(this, "FINALIZE");
    }
}
