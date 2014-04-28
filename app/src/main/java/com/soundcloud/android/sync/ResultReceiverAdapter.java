package com.soundcloud.android.sync;

import rx.Subscriber;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

/**
 * Bridge between an Android ResultReceiver and an Rx Subscriber
 */
class ResultReceiverAdapter extends ResultReceiver {

    private final Subscriber<? super Boolean> subscriber;
    final Uri contentUri;

    public ResultReceiverAdapter(Subscriber<? super Boolean> subscriber, Uri contentUri) {
        super(new Handler(Looper.getMainLooper()));
        this.subscriber = subscriber;
        this.contentUri = contentUri;
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_APPEND_FINISHED:
                final boolean dataUpdated = resultData.getBoolean(contentUri.toString());
                subscriber.onNext(dataUpdated);
                subscriber.onCompleted();
                break;
            case ApiSyncService.STATUS_SYNC_ERROR:
            case ApiSyncService.STATUS_APPEND_ERROR:
                subscriber.onError(new SyncFailedException(resultData));
                break;
            default:
                throw new IllegalStateException("Unexpected sync state: " + resultCode);
        }
    }
}
