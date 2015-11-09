package com.soundcloud.android.sync;

import com.soundcloud.android.utils.Log;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.annotation.VisibleForTesting;

public class ResultReceiverAdapter extends ResultReceiver {

    @VisibleForTesting
    public static final String SYNC_RESULT = "syncResult";
    private static final String TAG = "RxResultReceiver";

    private volatile Subscriber<? super SyncResult> subscriber;

    public ResultReceiverAdapter(final Subscriber<? super SyncResult> subscriber, Looper looper) {
        super(new Handler(looper));
        this.subscriber = subscriber;
        // make sure we release the observer reference as soon as we're unsubscribing
        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                Log.d(TAG, "observer is unsubscribing, releasing ref...");
                ResultReceiverAdapter.this.subscriber = null;
            }
        }));
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (subscriber != null && !subscriber.isUnsubscribed()) {
            Log.d(TAG, "delivering result: " + resultData);
            SyncResult syncResult = resultData.getParcelable(SYNC_RESULT);
            if (syncResult.wasSuccess()) {
                subscriber.onNext(syncResult);
                subscriber.onCompleted();
            } else {
                subscriber.onError(syncResult.getException());
            }
        } else {
            Log.d(TAG, "observer is gone, dropping result: " + resultData);
        }
    }
}
