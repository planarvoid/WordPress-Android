package com.soundcloud.android.rx.observers;

/**
 * Observer which basically determines if an operation was success or failure. Should only
 * be used with blocking observables.
 *
 */
public class SuccessObserver extends DefaultObserver<Object> {
    private boolean isSuccess;
    @Override
    public void onCompleted() {
        isSuccess = true;
    }

    public boolean wasSuccess(){
        return isSuccess;
    }
}
