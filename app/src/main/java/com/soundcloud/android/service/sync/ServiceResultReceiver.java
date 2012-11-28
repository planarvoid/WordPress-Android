package com.soundcloud.android.service.sync;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

class ServiceResultReceiver extends ResultReceiver {
    public static final int NOTIFICATION_MAX = 100;
    private static final String NOT_PLUS = (NOTIFICATION_MAX-1)+"+";

    private SyncResult result;
    private SoundCloudApplication app;
    private Bundle extras;

    public ServiceResultReceiver(SoundCloudApplication app, SyncResult result, Bundle extras) {
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
                    final long frequency = SyncConfig.getNotificationsFrequency(app);
                    final long delta = System.currentTimeMillis() -
                            app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_AT);
                    if (delta > frequency) {
                        final long lastIncomingSeen = app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN);
                        final Activities incoming = !SyncConfig.isIncomingEnabled(app, extras) ? Activities.EMPTY :
                                Activities.getSince(Content.ME_SOUND_STREAM, app.getContentResolver(), lastIncomingSeen);


                        maybeNotifyIncoming(app, incoming);
                    } else if (Log.isLoggable(SyncAdapterService.TAG, Log.DEBUG)) {
                            Log.d(SyncAdapterService.TAG, "skipping incoming notification, delta "+delta+" < frequency="+frequency);
                    }

                    final long lastOwnSeen = app.getAccountDataLong(User.DataKeys.LAST_OWN_SEEN);
                    final Activities news = !SyncConfig.isActivitySyncEnabled(app, extras) ? Activities.EMPTY :
                            Activities.getSince(Content.ME_ACTIVITIES, app.getContentResolver(), lastOwnSeen);
                    maybeNotifyOwn(app, news, extras);
                }
                break;
            }
        }
    }

    private boolean maybeNotifyIncoming(SoundCloudApplication app,
                                             Activities incoming) {

        final int totalUnseen = Activities.getUniqueTrackCount(incoming);
        final boolean hasIncoming = !incoming.isEmpty();

        if (totalUnseen > 0) {
            app.updateActivityUnseenCount(Content.ME_SOUND_STREAM,totalUnseen);
        }

        if (hasIncoming) {
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


            message = Message.getIncomingNotificationMessage(app, incoming);
            String artwork_url = incoming.getFirstAvailableArtwork();

            if (incoming.newerThan(app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM))) {
                prefetchArtwork(app, incoming);

                Message.showDashboardNotification(app, ticker, title, message, Message.createNotificationIntent(Actions.STREAM),
                        Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID, artwork_url);

                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, System.currentTimeMillis());
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM, incoming.getTimestamp());

                return true;
            } else return false;
        } else {
            if (Log.isLoggable(SyncAdapterService.TAG, Log.DEBUG)) Log.d(SyncAdapterService.TAG, "no items, skip track notfication");
            return false;
        }
    }

    private boolean maybeNotifyOwn(SoundCloudApplication app, Activities activities, Bundle extras) {
        if (!activities.isEmpty()) {
            app.updateActivityUnseenCount(Content.ME_ACTIVITIES,activities.size());

            boolean likeEnabled = SyncConfig.isLikeEnabled(app, extras);
            final boolean commentsEnabled = SyncConfig.isCommentsEnabled(app, extras);

            Activities favoritings = likeEnabled ? activities.trackLikes() : Activities.EMPTY;
            Activities comments    = commentsEnabled ? activities.comments() : Activities.EMPTY;

            Activities notifyable = Activities.EMPTY;
            if (likeEnabled && commentsEnabled){
                notifyable = activities.commentsAndTrackLikes();
            } else if (likeEnabled){
                notifyable = favoritings;
            } else if (commentsEnabled){
                notifyable = comments;
            }

            if (notifyable == null || notifyable.isEmpty()) return false;
            Message msg = new Message(app.getResources(), notifyable, favoritings, comments);

            if (activities.newerThan(app.getAccountDataLong(User.DataKeys.LAST_OWN_NOTIFIED_ITEM))) {
                prefetchArtwork(app, activities);

                Message.showDashboardNotification(app, msg.ticker, msg.title, msg.message,
                        Message.createNotificationIntent(Actions.ACTIVITY),
                        Consts.Notifications.DASHBOARD_NOTIFY_ACTIVITIES_ID,
                        activities.getFirstAvailableAvatar());

                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_ITEM, activities.getTimestamp());
                return true;
            } else return false;
        } else return false;
    }

    private int prefetchArtwork(Context context, Activities... activities) {
        if (IOUtils.isWifiConnected(context)) {
            Set<String> urls = new HashSet<String>();
            for (Activities a : activities) {
                urls.addAll(a.artworkUrls());
            }
            int tofetch = SyncAdapterService.MAX_ARTWORK_PREFETCH;
            for (String url : urls) {
                ImageLoader.get(context).prefetch(url);
                if (tofetch-- <= 0) break;
            }
            return Math.min(urls.size(), SyncAdapterService.MAX_ARTWORK_PREFETCH);
        } else {
            // prefetch artwork only when connected to wifi
            return 0;
        }
    }
}
