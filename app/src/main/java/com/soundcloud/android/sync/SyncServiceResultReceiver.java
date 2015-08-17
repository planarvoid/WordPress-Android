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
            maybeNotifyActivity(context, activities);
        }

    }



    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private boolean maybeNotifyActivity(Context context, Activities activities) {
        if (!activities.isEmpty()) {

            final Activities likes = getLikeNotifications(context, activities);
            final Activities comments = getCommentNotifications(context, activities);
            final Activities reposts = getRepostNotifications(context, activities);
            final Activities followers = getFollowersNotifications(context, activities);
            final Activities activitiesToNotify = Activities.EMPTY.merge(likes, comments, reposts, followers);

            if (activitiesToNotify.isEmpty()) {
                return false;
            }

            activitiesToNotify.sort();

            if (activitiesToNotify.newerThan(ContentStats.getLastNotifiedItem(context, Content.ME_ACTIVITIES))) {
                final NotificationMessage msg = new NotificationMessage
                        .Builder(context.getResources())
                        .setMixed(activitiesToNotify)
                        .setLikes(likes)
                        .setComments(comments)
                        .setReposts(reposts)
                        .setFollowers(followers)
                        .build();
                NotificationMessage.showDashboardNotification(context, msg.ticker, msg.title, msg.message,
                        NotificationMessage.createNotificationIntent(Actions.ACTIVITY),
                        NotificationConstants.DASHBOARD_NOTIFY_ACTIVITIES_ID,
                        activitiesToNotify.getFirstAvailableAvatar());

                ContentStats.setLastNotifiedItem(context, Content.ME_ACTIVITIES, activitiesToNotify.getTimestamp());
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private Activities getFollowersNotifications(Context context, Activities activities) {
        return SyncConfig.isNewFollowerNotificationsEnabled(context) ? activities.followers() : Activities.EMPTY;
    }

    private Activities getRepostNotifications(Context context, Activities activities) {
        return SyncConfig.isRepostNotificationsEnabled(context) ? activities.trackReposts() : Activities.EMPTY;
    }

    private Activities getCommentNotifications(Context context, Activities activities) {
        return SyncConfig.isCommentNotificationsEnabled(context) ? activities.comments() : Activities.EMPTY;
    }

    private Activities getLikeNotifications(Context context, Activities activities) {
        return SyncConfig.isLikeNotificationEnabled(context) ? activities.trackLikes() : Activities.EMPTY;
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
