package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.rx.observers.DetachableObserver;
import com.soundcloud.android.utils.Log;

abstract class ObservedRunnable<T> implements Runnable {

    private DetachableObserver<T> mObserver;

    @Override
    public final void run() {
        run(mObserver);
    }

    public void attachObserver(DetachableObserver<T> observer) {
        mObserver = observer;
    }

    public void detachObserver() {
        mObserver.detach();
    }

    protected abstract void run(DetachableObserver<T> observer);


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(this, "FINALIZE");
    }
}
