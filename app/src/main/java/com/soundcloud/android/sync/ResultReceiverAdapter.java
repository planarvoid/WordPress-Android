package com.soundcloud.android.sync;

import com.soundcloud.android.utils.Log;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.Disposables;

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

    private final AtomicReference<SingleEmitter<? super SyncJobResult>> subscriberRef;

    public ResultReceiverAdapter(final SingleEmitter<? super SyncJobResult> subscriber, Looper looper) {
        super(new Handler(looper));
        this.subscriberRef = new AtomicReference<>(subscriber);
        // make sure we release the observer reference as soon as we're unsubscribing, or
        // we might create a memory leak
        subscriber.setDisposable(Disposables.fromAction(() -> {
            Log.d(TAG, "observer is unsubscribing, releasing ref...");
            subscriberRef.set(null);
        }));
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        final SingleEmitter<? super SyncJobResult> subscriber = subscriberRef.get();
        if (subscriber != null && !subscriber.isDisposed()) {
            Log.d(TAG, "delivering result: " + resultData);
            SyncJobResult syncJobResult = resultData.getParcelable(SYNC_RESULT);
            if (syncJobResult.wasSuccess()) {
                subscriber.onSuccess(syncJobResult);
            } else {
                subscriber.onError(syncJobResult.getException());
            }
        } else {
            Log.d(TAG, "observer is gone, dropping result: " + resultData);
        }
    }
}
