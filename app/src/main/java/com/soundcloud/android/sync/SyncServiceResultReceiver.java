package com.soundcloud.android.sync;

import com.soundcloud.android.Actions;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.stream.SoundStreamSyncOperations;
import com.soundcloud.android.utils.Log;

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
class SyncServiceResultReceiver extends ResultReceiver {
    private final SoundStreamSyncOperations soundStreamSyncOperations;
    private final SyncStateManager syncStateManager;
    private final SyncResult result;
    private final Context context;
    private final Bundle extras;
    private final OnResultListener listener;


    private SyncServiceResultReceiver(Context context, SoundStreamSyncOperations soundStreamSyncOperations, SyncStateManager syncStateManager,
                                      SyncResult result, Bundle extras, OnResultListener listener) {
        super(new Handler());
        this.syncStateManager = syncStateManager;
        this.result = result;
        this.context = context;
        this.soundStreamSyncOperations = soundStreamSyncOperations;
        this.extras = extras;
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
        final ActivitiesStorage activitiesStorage = new ActivitiesStorage();
        final long frequency = SyncConfig.getNotificationsFrequency(context);
        final long delta = System.currentTimeMillis() - ContentStats.getLastNotified(context, Content.ME_SOUND_STREAM);

        // deliver incoming sounds, if the user has enabled this
        if (SyncConfig.isIncomingEnabled(context)) {
            if (delta > frequency) {
                soundStreamSyncOperations.createNotificationForUnseenItems();
            } else {
                Log.d(SyncAdapterService.TAG, "skipping stream notification, delta " + delta + " < frequency=" + frequency);
            }
        }

        // deliver incoming activities, if the user has enabled this
        if (SyncConfig.isActivitySyncEnabled(context, extras)) {
            final long lastOwnSeen = ContentStats.getLastSeen(context, Content.ME_ACTIVITIES);
            Activities activities = activitiesStorage.getCollectionSince(Content.ME_ACTIVITIES.uri, lastOwnSeen);
            maybeNotifyActivity(context, activities, extras);
        }

    }



    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private boolean maybeNotifyActivity(Context app, Activities activities, Bundle extras) {
        if (!activities.isEmpty()) {
            final boolean likeEnabled = SyncConfig.isLikeEnabled(app);
            final boolean commentsEnabled = SyncConfig.isCommentsEnabled(app);
            final boolean repostsEnabled = SyncConfig.isRepostEnabled(app);

            final Activities likes = likeEnabled ? activities.trackLikes() : Activities.EMPTY;
            final Activities comments = commentsEnabled ? activities.comments() : Activities.EMPTY;
            final Activities reposts = repostsEnabled ? activities.trackReposts() : Activities.EMPTY;

            Activities notifyable = new Activities();
            if (likeEnabled) {
                notifyable = notifyable.merge(likes);
            }
            if (commentsEnabled) {
                notifyable = notifyable.merge(comments);
            }
            if (repostsEnabled) {
                notifyable = notifyable.merge(reposts);
            }

            if (notifyable.isEmpty()) {
                return false;
            }
            notifyable.sort();

            if (notifyable.newerThan(ContentStats.getLastNotifiedItem(app, Content.ME_ACTIVITIES))) {
                NotificationMessage msg = new NotificationMessage(app.getResources(), notifyable, likes, comments, reposts);
                NotificationMessage.showDashboardNotification(app, msg.ticker, msg.title, msg.message,
                        NotificationMessage.createNotificationIntent(Actions.ACTIVITY),
                        NotificationConstants.DASHBOARD_NOTIFY_ACTIVITIES_ID,
                        notifyable.getFirstAvailableAvatar());

                ContentStats.setLastNotifiedItem(app, Content.ME_ACTIVITIES, notifyable.getTimestamp());
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public interface OnResultListener {
        void onResultReceived();
    }

    public static class Factory {
        private final Context context;
        private final SoundStreamSyncOperations soundStreamSyncOps;
        private final SyncStateManager syncStateManager;

        @Inject
        public Factory(Context context, SoundStreamSyncOperations soundStreamSyncOps, SyncStateManager syncStateManager) {
            this.context = context;
            this.soundStreamSyncOps = soundStreamSyncOps;
            this.syncStateManager = syncStateManager;
        }

        public SyncServiceResultReceiver create(SyncResult result, Bundle extras, OnResultListener listener){
            return new SyncServiceResultReceiver(context, soundStreamSyncOps, syncStateManager, result, extras, listener);
        }
    }
}
