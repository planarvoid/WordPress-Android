package com.soundcloud.android.sync;

import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.stream.SoundStreamNotifier;
import com.soundcloud.android.sync.activities.ActivitiesNotifier;
import com.soundcloud.android.utils.Log;

import android.annotation.SuppressLint;
import android.content.Context;
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
    private final SoundStreamNotifier soundStreamNotifier;
    private final ActivitiesNotifier activitiesNotifier;
    private final SyncStateManager syncStateManager;
    private final ContentStats contentStats;
    private final SyncResult result;
    private final Context context;
    private final OnResultListener listener;


    private SyncServiceResultReceiver(Context context,
                                      SoundStreamNotifier soundStreamNotifier,
                                      ActivitiesNotifier activitiesNotifier,
                                      SyncStateManager syncStateManager,
                                      ContentStats contentStats,
                                      SyncResult result,
                                      OnResultListener listener) {
        super(new Handler());
        this.activitiesNotifier = activitiesNotifier;
        this.syncStateManager = syncStateManager;
        this.contentStats = contentStats;
        this.result = result;
        this.context = context;
        this.soundStreamNotifier = soundStreamNotifier;
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

                // notification related
                if (SyncConfig.shouldUpdateDashboard(context)) {
                    createSystemNotification();
                }
                break;
            }
        }
    }

    private void createSystemNotification() {
        final long frequency = SyncConfig.getNotificationsFrequency(context);
        final long delta = System.currentTimeMillis() - contentStats.getLastNotified(Content.ME_SOUND_STREAM);

        // deliver incoming sounds, if the user has enabled this
        if (SyncConfig.isIncomingEnabled(context)) {
            if (delta > frequency) {
                soundStreamNotifier.notifyUnseenItems();
            } else {
                Log.d(SyncAdapterService.TAG, "skipping stream notification, delta " + delta + " < frequency=" + frequency);
            }
        }

        // deliver incoming activities, if the user has enabled this
        if (SyncConfig.isActivitySyncEnabled(context)) {
            activitiesNotifier.notifyUnseenItems(context);
        }
    }

    public interface OnResultListener {
        void onResultReceived();
    }

    public static class Factory {
        private final Context context;
        private final SoundStreamNotifier streamNotifier;
        private final ActivitiesNotifier activitiesNotifier;
        private final SyncStateManager syncStateManager;
        private final ContentStats contentStats;

        @Inject
        public Factory(Context context, SoundStreamNotifier streamNotifier,
                       ActivitiesNotifier activitiesNotifier, SyncStateManager syncStateManager,
                       ContentStats contentStats) {
            this.context = context;
            this.streamNotifier = streamNotifier;
            this.activitiesNotifier = activitiesNotifier;
            this.syncStateManager = syncStateManager;
            this.contentStats = contentStats;
        }

        public SyncServiceResultReceiver create(SyncResult result, OnResultListener listener) {
            return new SyncServiceResultReceiver(context, streamNotifier, activitiesNotifier,
                    syncStateManager, contentStats, result, listener);
        }
    }
}
