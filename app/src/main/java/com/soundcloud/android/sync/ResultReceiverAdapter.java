package com.soundcloud.android.sync;

import rx.Observer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.annotation.VisibleForTesting;

public class ResultReceiverAdapter extends ResultReceiver {

    @VisibleForTesting
    public static final String SYNC_RESULT = "syncResult";

    private final Observer<? super SyncResult> observer;

    public ResultReceiverAdapter(Observer<? super SyncResult> observer, Looper looper) {
        super(new Handler(looper));
        this.observer = observer;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        SyncResult syncResult = resultData.getParcelable(SYNC_RESULT);
        if (syncResult.wasSuccess()){
            observer.onNext(syncResult);
            observer.onCompleted();
        } else {
            observer.onError(syncResult.getException());
        }
    }
}
