package com.soundcloud.android.sync;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.utils.ErrorUtils;

import android.annotation.SuppressLint;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressLint("ParcelCreator")
@AutoFactory
class BackgroundSyncResultReceiver extends ResultReceiver {

    private static final int RANDOMIZED_RETRY_MINIMUM = 10;
    private static final int RANDOMIZED_RETRY_RANGE = 20;

    private final Runnable syncCompleteRunnable;
    private final SyncResult syncResult;
    private final SyncStateStorage syncStateStorage;

    public BackgroundSyncResultReceiver(Runnable syncCompleteRunnable,
                                        SyncResult syncResult,
                                        @Provided SyncStateStorage syncStateStorage) {
        super(new Handler());
        this.syncCompleteRunnable = syncCompleteRunnable;
        this.syncResult = syncResult;
        this.syncStateStorage = syncStateStorage;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);

        for (String resultKey : resultData.keySet()) {
            final Syncable syncable = Syncable.valueOf(resultKey);
            SyncJobResult result = resultData.getParcelable(resultKey);
            if (result.wasSuccess()) {
                udpateSyncMisses(syncable, result);
            } else {
                calculateResultFromErrors(result);
            }
        }

        syncCompleteRunnable.run();
    }

    private void udpateSyncMisses(Syncable syncable, SyncJobResult result) {
        if (result.wasChanged()) {
            syncStateStorage.resetSyncMisses(syncable);
        } else {
            syncStateStorage.incrementSyncMisses(syncable);
        }
    }

    private void calculateResultFromErrors(SyncJobResult result) {
        final Exception exception = result.getException();
        if (exception instanceof ApiRequestException) {
            handleApiRequestException(((ApiRequestException) exception));
        } else {
            ErrorUtils.handleSilentException(exception);
        }
    }


    private void handleApiRequestException(ApiRequestException exception) {
        switch (exception.reason()) {
            case AUTH_ERROR:
            case NOT_ALLOWED:
                syncResult.stats.numAuthExceptions++;
                break;

            case NETWORK_ERROR:
                syncResult.stats.numIoExceptions++;
                break;

            case SERVER_ERROR:
                syncResult.delayUntil = getRandomizedDelayTime();
                break;

            default:
                ErrorUtils.handleSilentException(exception);
                break;
        }
    }

    private static long getRandomizedDelayTime() {
        return TimeUnit.MINUTES.toSeconds(RANDOMIZED_RETRY_MINIMUM + new Random().nextInt(RANDOMIZED_RETRY_RANGE));
    }
}
