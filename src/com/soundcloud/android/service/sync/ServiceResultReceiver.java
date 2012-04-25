package com.soundcloud.android.service.sync;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.text.TextUtils;
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
                Looper.myLooper().quit();
                break;
            }
            case ApiSyncService.STATUS_SYNC_FINISHED: {

                SyncContent.updateCollections(app,resultData);

                if (SyncConfig.shouldUpdateDashboard(app)) {
                    final long frequency = SyncConfig.getNotificationsFrequency(app);
                    final long delta = System.currentTimeMillis() -
                            app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_AT);
                    if (delta > frequency) {
                        final long lastIncomingSeen = app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN);
                        final Activities incoming = !SyncConfig.isIncomingEnabled(app, extras) ? Activities.EMPTY :
                                Activities.getSince(Content.ME_SOUND_STREAM, app.getContentResolver(), lastIncomingSeen);

                        final Activities exclusive = !SyncConfig.isExclusiveEnabled(app, extras) ? Activities.EMPTY
                                : Activities.getSince(Content.ME_EXCLUSIVE_STREAM, app.getContentResolver(), lastIncomingSeen);

                        maybeNotifyIncoming(app, incoming, exclusive);
                    } else if (Log.isLoggable(SyncAdapterService.TAG, Log.DEBUG)) {
                            Log.d(SyncAdapterService.TAG, "skipping incoming notification, delta "+delta+" < frequency="+frequency);
                    }

                    final long lastOwnSeen = app.getAccountDataLong(User.DataKeys.LAST_OWN_SEEN);
                    final Activities news = !SyncConfig.isActivitySyncEnabled(app, extras) ? Activities.EMPTY :
                            Activities.getSince(Content.ME_ACTIVITIES, app.getContentResolver(), lastOwnSeen);
                    maybeNotifyOwn(app, news, extras);
                }
                Looper.myLooper().quit();
                break;
            }
        }
    }



    private boolean maybeNotifyIncoming(SoundCloudApplication app,
                                             Activities incoming,
                                             Activities exclusive) {

        final int totalUnseen = Activities.getUniqueTrackCount(incoming, exclusive);
        final boolean hasIncoming = !incoming.isEmpty();
        final boolean hasExclusive = !exclusive.isEmpty();
        if (hasIncoming || hasExclusive) {
            final CharSequence title, message, ticker;
            String artwork_url = null;

            if (totalUnseen == 1) {
                ticker = app.getString(R.string.dashboard_notifications_ticker_single);
                title = app.getString(R.string.dashboard_notifications_title_single);
            } else {
                ticker = String.format(app.getString(
                        R.string.dashboard_notifications_ticker), totalUnseen >= NOTIFICATION_MAX ? NOT_PLUS : totalUnseen);

                title = String.format(app.getString(
                        R.string.dashboard_notifications_title), totalUnseen >= NOTIFICATION_MAX ? NOT_PLUS : totalUnseen);
            }

            if (hasExclusive) {
                message = Message.getExclusiveNotificationMessage(app, exclusive);
                artwork_url = exclusive.getFirstAvailableArtwork();
            } else {
                message = Message.getIncomingNotificationMessage(app, incoming);
            }

            // either no exclusive or no exclusive artwork
            if (TextUtils.isEmpty(artwork_url)) {
                artwork_url = incoming.getFirstAvailableArtwork();
            }

            if (incoming.newerThan(app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM)) ||
                    exclusive.newerThan(app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM))) {
                prefetchArtwork(app, incoming, exclusive);

                Message.showDashboardNotification(app, ticker, title, message, Message.createNotificationIntent(Actions.STREAM),
                        Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID, artwork_url);

                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, System.currentTimeMillis());
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM,
                        Math.max(incoming.getTimestamp(), exclusive.getTimestamp()));

                return true;
            } else return false;
        } else {
            if (Log.isLoggable(SyncAdapterService.TAG, Log.DEBUG)) Log.d(SyncAdapterService.TAG, "no items, skip track notfication");
            return false;
        }
    }

    private boolean maybeNotifyOwn(SoundCloudApplication app, Activities activities, Bundle extras) {
        if (!activities.isEmpty()) {
            Activities favoritings = SyncConfig.isLikeEnabled(app, extras) ? activities.favoritings() : Activities.EMPTY;
            Activities comments    = SyncConfig.isCommentsEnabled(app, extras) ? activities.comments() : Activities.EMPTY;

            Message msg = new Message(app.getResources(), activities, favoritings, comments);

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
