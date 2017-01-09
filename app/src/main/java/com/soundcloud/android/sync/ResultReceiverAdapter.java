package com.soundcloud.android.sync;

import com.soundcloud.android.utils.Log;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.atomic.AtomicReference;

@SuppressLint("ParcelCreator") // not sure how to fix this; needs review
public class ResultReceiverAdapter extends ResultReceiver {

    @VisibleForTesting
    public static final String SYNC_RESULT = "syncResult";
    private static final String TAG = "RxResultReceiver";

    private final AtomicReference<Subscriber<? super SyncJobResult>> subscriberRef;

    public ResultReceiverAdapter(final Subscriber<? super SyncJobResult> subscriber, Looper looper) {
        super(new Handler(looper));
        this.subscriberRef = new AtomicReference<Subscriber<? super SyncJobResult>>(subscriber);
        // make sure we release the observer reference as soon as we're unsubscribing, or
        // we might create a memory leak
        subscriber.add(Subscriptions.create(() -> {
            Log.d(TAG, "observer is unsubscribing, releasing ref...");
            subscriberRef.set(null);
        }));
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        final Subscriber<? super SyncJobResult> subscriber = subscriberRef.get();
        if (subscriber != null && !subscriber.isUnsubscribed()) {
            Log.d(TAG, "delivering result: " + resultData);
            SyncJobResult syncJobResult = resultData.getParcelable(SYNC_RESULT);
            if (syncJobResult.wasSuccess()) {
                subscriber.onNext(syncJobResult);
                subscriber.onCompleted();
            } else {
                subscriber.onError(syncJobResult.getException());
            }
        } else {
            Log.d(TAG, "observer is gone, dropping result: " + resultData);
        }
    }
}
