package com.soundcloud.android.sync;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.Log;

import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Receives and processes the results from a sync run initiated in {@link SyncAdapterService}, creating
 * notifications if necessary.
 */
class SyncServiceResultReceiver extends ResultReceiver {
    public static final int NOTIFICATION_MAX = 100;
    private static final String NOT_PLUS = (NOTIFICATION_MAX - 1) + "+";

    private final SyncResult result;
    private final SoundCloudApplication app;
    private final Bundle extras;

    public SyncServiceResultReceiver(SoundCloudApplication app, SyncResult result, Bundle extras) {
        super(new Handler());
        this.result = result;
        this.app = app;
        this.extras = extras;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_ERROR: {
                SyncResult serviceResult = resultData.getParcelable(ApiSyncService.EXTRA_SYNC_RESULT);
                result.stats.numAuthExceptions = serviceResult.stats.numAuthExceptions;
                result.stats.numIoExceptions = serviceResult.stats.numIoExceptions;
                break;
            }

            case ApiSyncService.STATUS_SYNC_FINISHED: {
                SyncContent.updateCollections(app, resultData);

                // notification related
                if (SyncConfig.shouldUpdateDashboard(app)) {
                    createSystemNotification();
                }
                break;
            }
        }
    }

    private void createSystemNotification() {
        final ActivitiesStorage activitiesStorage = new ActivitiesStorage();
        final long frequency = SyncConfig.getNotificationsFrequency(app);
        final long delta = System.currentTimeMillis() - ContentStats.getLastNotified(app, Content.ME_SOUND_STREAM);

        // deliver incoming sounds, if the user has enabled this
        if (SyncConfig.isIncomingEnabled(app, extras)) {
            if (delta > frequency) {
                final long lastStreamSeen = ContentStats.getLastSeen(app, Content.ME_SOUND_STREAM);
                Activities activities = activitiesStorage.getCollectionSince(Content.ME_SOUND_STREAM.uri, lastStreamSeen);
                maybeNotifyStream(app, activities);

            } else {
                Log.d(SyncAdapterService.TAG, "skipping stream notification, delta " + delta + " < frequency=" + frequency);
            }
        }

        // deliver incoming activities, if the user has enabled this
        if (SyncConfig.isActivitySyncEnabled(app, extras)) {
            final long lastOwnSeen = ContentStats.getLastSeen(app, Content.ME_ACTIVITIES);
            Activities activities = activitiesStorage.getCollectionSince(Content.ME_ACTIVITIES.uri, lastOwnSeen);
            maybeNotifyActivity(app, activities, extras);
        }

    }

    private boolean maybeNotifyStream(SoundCloudApplication app, Activities stream) {
        final int totalUnseen = Activities.getUniqueTrackCount(stream);
        if (totalUnseen > 0) {
            ContentStats.updateCount(app, Content.ME_SOUND_STREAM, totalUnseen);
        }
        if (!stream.isEmpty() && stream.newerThan(ContentStats.getLastNotified(app, Content.ME_SOUND_STREAM))) {
            final CharSequence title, message, ticker;
            if (totalUnseen == 1) {
                ticker = app.getString(R.string.dashboard_notifications_ticker_single);
                title = app.getString(R.string.dashboard_notifications_title_single);
            } else {
                ticker = String.format(app.getString(
                        R.string.dashboard_notifications_ticker), totalUnseen >= NOTIFICATION_MAX ? NOT_PLUS : totalUnseen);

                title = String.format(app.getString(
                        R.string.dashboard_notifications_title), totalUnseen >= NOTIFICATION_MAX ? NOT_PLUS : totalUnseen);
            }


            message = NotificationMessage.getIncomingNotificationMessage(app, stream);
            String artwork_url = stream.getFirstAvailableArtwork();

            final Intent intent = NotificationMessage.createNotificationIntent(Actions.STREAM);
            intent.putExtra(MainActivity.EXTRA_REFRESH_STREAM, true);
            NotificationMessage.showDashboardNotification(app, ticker, title, message, intent,
                    Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID, artwork_url);

            ContentStats.setLastNotified(app, Content.ME_SOUND_STREAM, System.currentTimeMillis());
            ContentStats.setLastNotifiedItem(app, Content.ME_SOUND_STREAM, stream.getTimestamp());

            return true;
        } else {
            Log.d(SyncAdapterService.TAG, "no new items, skip track notfication");
            return false;
        }
    }

    private boolean maybeNotifyActivity(SoundCloudApplication app, Activities activities, Bundle extras) {
        if (!activities.isEmpty()) {
            ContentStats.updateCount(app, Content.ME_ACTIVITIES, activities.size());

            final boolean likeEnabled = SyncConfig.isLikeEnabled(app, extras);
            final boolean commentsEnabled = SyncConfig.isCommentsEnabled(app, extras);
            final boolean repostsEnabled = SyncConfig.isRepostEnabled(app, extras);

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
                        Consts.Notifications.DASHBOARD_NOTIFY_ACTIVITIES_ID,
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
}
