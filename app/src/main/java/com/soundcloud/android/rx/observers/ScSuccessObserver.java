package com.soundcloud.android.rx.observers;

import com.soundcloud.android.utils.Log;

/**
 * Observer which basically determines if an operation was success or failure. Should only
 * be used with blocking observables.
 *
 */
public class ScSuccessObserver<T> extends ScObserver<T> {
    private static String TAG = ScSuccessObserver.class.getName();
    private boolean isSuccess;
    @Override
    public void onCompleted() {
        isSuccess = true;
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Problem occured when trying execute success observer", e);
    }

    public boolean wasSuccess(){
        return isSuccess;
    }
}
