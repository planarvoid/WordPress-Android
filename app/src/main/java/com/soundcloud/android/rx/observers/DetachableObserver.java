package com.soundcloud.android.rx.observers;

import com.soundcloud.android.utils.Log;
import rx.Observer;

public class DetachableObserver<T> implements Observer<T> {

    private Observer<T> mObserver;

    public DetachableObserver(Observer<T> observer) {
        mObserver = observer;
    }

    @Override
    public void onCompleted() {
        if (mObserver != null) {
            mObserver.onCompleted();
        }
    }

    @Override
    public void onError(Exception e) {
        if (mObserver != null) {
            mObserver.onError(e);
        }
    }

    @Override
    public void onNext(T args) {
        if (mObserver != null) {
            mObserver.onNext(args);
        }
    }

    public void detach() {
        mObserver = null;
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(this, "FINALIZE");
    }
}
