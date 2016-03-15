package com.soundcloud.android.sync;

import android.annotation.SuppressLint;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import javax.inject.Inject;

/**
 * Receives and processes the results from a sync run initiated in {@link SyncAdapterService}, creating
 * notifications if necessary.
 */
@SuppressLint("ParcelCreator") // we need to review this; not an easy fix
class SyncServiceResultReceiver extends ResultReceiver {
    private final SyncStateManager syncStateManager;
    private final SyncResult result;
    private final OnResultListener listener;

    private SyncServiceResultReceiver(SyncStateManager syncStateManager,
                                      SyncResult result,
                                      OnResultListener listener) {
        super(new Handler());
        this.syncStateManager = syncStateManager;
        this.result = result;
        this.listener = listener;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        try {
            handleSyncResult(resultCode, resultData);
        } finally {
            // This listener must be called under every circumstance, after the result is handled.
            // Not calling it could result in holding a wakelock indefiinitely
            if (listener != null) {
                listener.onResultReceived();
            }
        }
    }

    private void handleSyncResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_ERROR: {
                SyncResult serviceResult = resultData.getParcelable(ApiSyncService.EXTRA_SYNC_RESULT);
                result.stats.numAuthExceptions = serviceResult.stats.numAuthExceptions;
                result.stats.numIoExceptions = serviceResult.stats.numIoExceptions;
                result.delayUntil = serviceResult.delayUntil;
                break;
            }

            case ApiSyncService.STATUS_SYNC_FINISHED: {
                SyncContent.updateCollections(syncStateManager, resultData);
                break;
            }
        }
    }

    public interface OnResultListener {
        void onResultReceived();
    }

    public static class Factory {
        private final SyncStateManager syncStateManager;

        @Inject
        public Factory(SyncStateManager syncStateManager) {
            this.syncStateManager = syncStateManager;
        }

        public SyncServiceResultReceiver create(SyncResult result, OnResultListener listener) {
            return new SyncServiceResultReceiver(syncStateManager, result, listener);
        }
    }
}
