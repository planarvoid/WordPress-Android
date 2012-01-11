package com.soundcloud.android.service.sync;

import android.content.*;
import android.os.*;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SyncAdapterService extends Service {
    private static final String TAG = SoundCloudApplication.class.getSimpleName();
    private ScSyncAdapter mSyncAdapter;

    public static final int NOTIFICATION_MAX = 100;
    private static final String NOT_PLUS = (NOTIFICATION_MAX-1)+"+";

    private static final long DEFAULT_NOTIFICATIONS_FREQUENCY = 14400; //60*60*4
    public static final long DEFAULT_POLL_FREQUENCY = 3600; //60*60*4

    private static final long DEFAULT_DELAY = 3600000; //60*60*1000 1 hr in ms
    private static final long TRACK_SYNC_DELAY = DEFAULT_DELAY;
    private static final long USER_SYNC_DELAY = DEFAULT_DELAY * 4; // every 2 hours, users aren't as crucial
    private static final long CLEANUP_DELAY = DEFAULT_DELAY * 24; // every 24 hours

    public enum SyncContent {
        MySounds(Content.ME_TRACKS, TRACK_SYNC_DELAY, "syncMySounds"),
        MyFavorites(Content.ME_FAVORITES, TRACK_SYNC_DELAY, "syncMyFavorites"),
        MyFollowings(Content.ME_FOLLOWINGS, USER_SYNC_DELAY, "syncMyFollowings"),
        MyFollowers(Content.ME_FOLLOWERS, USER_SYNC_DELAY, "syncMyFollowers");

        SyncContent(Content content, long syncDelay, String syncEnabledKey) {
            this.content = content;
            this.syncDelay = syncDelay;
            this.prefSyncEnabledKey = syncEnabledKey;
        }

        public final Content content;
        public final long syncDelay;
        public final String prefSyncEnabledKey;

        public static void configureSyncExtrasArray(Context c, List<String> urisToSync, boolean force){
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
            for (SyncContent sc : SyncContent.values()){
                if (sp.getBoolean(sc.prefSyncEnabledKey, true)) {
                    final long lastUpdated = LocalCollection.getLastSync(c.getContentResolver(), sc.content.uri);
                    if (System.currentTimeMillis() - lastUpdated > sc.syncDelay || force){
                        urisToSync.add(sc.content.uri.toString());
                    }
                }
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mSyncAdapter = new ScSyncAdapter((SoundCloudApplication) getApplication());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mSyncAdapter.getSyncAdapterBinder();
    }

    public static class ScSyncAdapter extends AbstractThreadedSyncAdapter {
        private final SoundCloudApplication mApp;

        public ScSyncAdapter(SoundCloudApplication app) {
            super(app, true);
            mApp = app;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                                  ContentProviderClient provider, SyncResult syncResult) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPerformSync("+account+","+extras+","+authority+","+provider+","+syncResult+")");
            }

            if (shouldUpdateDashboard(mApp) || shouldSyncCollections(mApp)) {
                SyncAdapterService.performSync(mApp, account, extras, provider, syncResult);
            } else {
                Log.d(TAG, "skipping sync because Wifi is diabled");
            }
            Log.i(TAG,"Done with sync " + syncResult);
        }
    }

    /** @noinspection UnusedParameters*/
    /* package */ static void performSync(final SoundCloudApplication app,
                                    Account account,
                                    Bundle extras,
                                    ContentProviderClient provider,
                                    final SyncResult syncResult) {
        if (app.useAccount(account).valid()) {
            final boolean force = extras.getBoolean(ContentResolver.SYNC_EXTRAS_FORCE, false);
            final Intent intent = new Intent(app,ApiSyncService.class);
            intent.setAction(ApiSyncService.SYNC_COLLECTION_ACTION);
            ArrayList<String> urisToSync = new ArrayList<String>();

            if (app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN) <= 0) {
                final long now = System.currentTimeMillis();
                app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, now);
                app.setAccountData(User.DataKeys.LAST_OWN_SEEN, now);
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, now);
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_AT, now);
            }

            if (shouldUpdateDashboard(app)) {
                if (isIncomingEnabled(app)) urisToSync.add(Content.ME_SOUND_STREAM.uri.toString());
                if (isExclusiveEnabled(app)) urisToSync.add(Content.ME_EXCLUSIVE_STREAM.uri.toString());
                if (isActivitySyncEnabled(app)) urisToSync.add(Content.ME_ACTIVITIES.uri.toString());
            }

            if (shouldSyncCollections(app)) {
                SyncContent.configureSyncExtrasArray(app, urisToSync, force);
            }

            final long lastCleanup = PreferenceManager.getDefaultSharedPreferences(app).getLong("lastSyncCleanup", System.currentTimeMillis());
            if (System.currentTimeMillis() - lastCleanup > CLEANUP_DELAY || force) {
                urisToSync.add(Content.TRACK_CLEANUP.uri.toString());
                urisToSync.add(Content.USERS_CLEANUP.uri.toString());
            }
            intent.putStringArrayListExtra("syncUris", urisToSync);
            Looper.prepare();
            intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ResultReceiver(new Handler()) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    switch (resultCode) {
                        case ApiSyncService.STATUS_RUNNING: {
                            break;
                        }
                        case ApiSyncService.STATUS_SYNC_ERROR: {
                            SyncResult serviceResult = resultData.getParcelable(ApiSyncService.EXTRA_SYNC_RESULT);
                            syncResult.stats.numAuthExceptions = serviceResult.stats.numAuthExceptions;
                            syncResult.stats.numIoExceptions = serviceResult.stats.numIoExceptions;
                            Looper.myLooper().quit();
                            break;
                        }
                        case ApiSyncService.STATUS_SYNC_FINISHED: {
                            if (shouldUpdateDashboard(app)) {
                                try {
                                    final long notificationsFrequency = getNotificationsFrequency(app) * 1000;
                                    if (System.currentTimeMillis() - app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_AT) > notificationsFrequency) {
                                        final long lastIncomingSeen = app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN);
                                        final File incomingFile = ActivitiesCache.getCacheFile(app, Request.to(Endpoints.MY_ACTIVITIES));
                                        final Activities incoming = !isIncomingEnabled(app) || !incomingFile.exists() ? Activities.EMPTY
                                                : Activities.fromJSON(incomingFile);

                                        final File exclusivesFile = ActivitiesCache.getCacheFile(app, Request.to(Endpoints.MY_EXCLUSIVE_TRACKS));
                                        final Activities exclusive = !isExclusiveEnabled(app) || !exclusivesFile.exists() ? Activities.EMPTY
                                                : Activities.fromJSON(exclusivesFile);

                                        checkIncoming(app, incoming.filter(lastIncomingSeen), exclusive.filter(lastIncomingSeen));
                                    }

                                    if (System.currentTimeMillis() - app.getAccountDataLong(User.DataKeys.LAST_OWN_NOTIFIED_AT) > notificationsFrequency) {
                                        final File activityFile = ActivitiesCache.getCacheFile(app, Request.to(Endpoints.MY_NEWS));
                                        final Activities news = !isActivitySyncEnabled(app) || !activityFile.exists() ? Activities.EMPTY
                                                : Activities.fromJSON(activityFile);

                                        checkOwn(app, news.filter(app.getAccountDataLong(User.DataKeys.LAST_OWN_SEEN)));
                                    }

                                } catch (IOException e) {
                                    Log.w(TAG, e);
                                    syncResult.stats.numIoExceptions++;
                                }
                            }
                            Looper.myLooper().quit();
                            break;
                        }
                    }
                }
            });

            app.startService(intent);
            Looper.loop();
        } else {
            Log.w(TAG, "no valid token, skip sync");
            syncResult.stats.numAuthExceptions++;
        }

    }

    /* package */ private static void checkIncoming(SoundCloudApplication app, Activities incoming, Activities exclusive) {

        final int totalUnseen = Activities.getUniqueTrackCount(incoming, exclusive);
        final boolean hasIncoming  = !incoming.isEmpty();
        final boolean hasExclusive = !exclusive.isEmpty();
        if (hasIncoming || hasExclusive) {
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

            if (hasExclusive) {
                message = getExclusiveMessaging(app, exclusive);
            } else {
                message = getIncomingMessaging(app, incoming);
            }

            if (incoming.newerThan(app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM)) ||
                exclusive.newerThan(app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM))) {

                createDashboardNotification(app, ticker, title, message,
                    Actions.STREAM,
                    Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID);

                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, System.currentTimeMillis());
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM,
                        Math.max(incoming.getTimestamp(), exclusive.getTimestamp()));
            }
        }
    }
    static Activities getOwnEvents(SoundCloudApplication app, Account account) throws IOException {
        return ActivitiesCache.get(app, account, Request.to(Endpoints.MY_NEWS));
    }

    /* package */ private static void checkOwn(SoundCloudApplication app, Activities events) {
        if (!events.isEmpty()) {
            Activities favoritings = isFavoritingEnabled(app) ? events.favoritings() : Activities.EMPTY;
            Activities comments    = isCommentsEnabled(app) ? events.comments() : Activities.EMPTY;

            Message msg = new Message(app.getResources(), events, favoritings, comments);

            if (events.newerThan(app.getAccountDataLong(User.DataKeys.LAST_OWN_NOTIFIED_ITEM))) {
                createDashboardNotification(app, msg.ticker, msg.title, msg.message, Actions.ACTIVITY,
                    Consts.Notifications.DASHBOARD_NOTIFY_ACTIVITIES_ID);

                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_AT, System.currentTimeMillis());
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_ITEM, events.getTimestamp());
            }
        }
    }

    /* package */ static Activities getNewIncomingEvents(SoundCloudApplication app, Account account, boolean exclusive)
            throws IOException {
        if ((!exclusive && !isIncomingEnabled(app)) || (exclusive && !isExclusiveEnabled(app))) {
            return Activities.EMPTY;
        } else {
            return ActivitiesCache.get(app, account,
                Request.to(exclusive ? Endpoints.MY_EXCLUSIVE_TRACKS : Endpoints.MY_ACTIVITIES));
        }
    }

    /* package */ static String getIncomingMessaging(SoundCloudApplication app, Activities activites) {
        List<User> users = activites.getUniqueUsers();
        assert !users.isEmpty();

        switch (users.size()) {
            case 1:
                return String.format(
                        app.getString(R.string.dashboard_notifications_message_incoming),
                        users.get(0).username);
            case 2:
                return String.format(
                        app.getString(R.string.dashboard_notifications_message_incoming_2),
                        users.get(0).username, users.get(1).username);
            default:
                return String.format(
                        app.getString(R.string.dashboard_notifications_message_incoming_others),
                        users.get(0).username, users.get(1).username);

        }
    }

    /* package */ static String getExclusiveMessaging(SoundCloudApplication app, Activities activities) {
        if (activities.size() == 1) {
            return String.format(
                    app.getString(R.string.dashboard_notifications_message_single_exclusive),
                    activities.get(0).getTrack().user.username);

        } else {
            List<User> users = activities.getUniqueUsers();
            assert !users.isEmpty();

            switch (users.size()) {
                case 1:
                    return String.format(
                            app.getString(R.string.dashboard_notifications_message_exclusive),
                            users.get(0).username);
                case 2:
                    return String.format(
                            app.getString(R.string.dashboard_notifications_message_exclusive_2),
                            users.get(0).username, users.get(1).username);
                default:


                    return String.format(app
                            .getString(R.string.dashboard_notifications_message_exclusive_others),
                            users.get(0).username, users.get(1).username);
            }
        }
    }

    private static void createDashboardNotification(Context context,
                                                    CharSequence ticker,
                                                    CharSequence title,
                                                    CharSequence message, String action, int id) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = (new Intent(action))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context.getApplicationContext(), 0, intent, 0);

        Notification n = new Notification(R.drawable.statusbar, ticker, System.currentTimeMillis());
        n.contentIntent = pi;
        n.flags = Notification.FLAG_AUTO_CANCEL;
        n.setLatestEventInfo(context.getApplicationContext(), title, message, pi);
        nm.notify(id, n);
    }

    // only used for debugging
    public static void requestNewSync(SoundCloudApplication app, int clearMode) {
        switch (clearMode){
            case 0:
                app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1);
                app.setAccountData(User.DataKeys.LAST_OWN_SEEN, 1);
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_ITEM, 1);
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM, 1);
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, 1);
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_AT, 1);
                ActivitiesCache.clear(app);
                break;
            case 1:
                app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN) - (24 * 3600000));
                app.setAccountData(User.DataKeys.LAST_OWN_SEEN, app.getAccountDataLong(User.DataKeys.LAST_OWN_SEEN) - (24 * 3600000));
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_ITEM, app.getAccountDataLong(User.DataKeys.LAST_OWN_NOTIFIED_ITEM) - (24 * 3600000));
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM, app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM) - (24 * 3600000));
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, app.getAccountDataLong(User.DataKeys.LAST_OWN_NOTIFIED_ITEM) - (24 * 3600000));
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_AT, app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM) - (24 * 3600000));
                ActivitiesCache.clear(app);
                break;
        }
        ContentResolver.requestSync(app.getAccount(), ScContentProvider.AUTHORITY, new Bundle());
    }

    private static boolean shouldUpdateDashboard(Context c) {
        return (!isNotificationsWifiOnlyEnabled(c) || CloudUtils.isWifiConnected(c));
    }

    private static boolean shouldSyncCollections(Context c) {
        return (!isSyncWifiOnlyEnabled(c) || CloudUtils.isWifiConnected(c));
    }

    private static boolean isNotificationsWifiOnlyEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsWifiOnly", false);
    }

    private static boolean isIncomingEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsIncoming", true);
    }

    private static boolean isExclusiveEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsExclusive", true);
    }

    private static boolean isFavoritingEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsFavoritings", true);
    }

    private static boolean isActivitySyncEnabled(Context c) {
        return isFavoritingEnabled(c) || isCommentsEnabled(c);
    }

    private static boolean isCommentsEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsComments", true);
    }

    private static boolean isSyncWifiOnlyEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("syncWifiOnly", true);
    }

    private static long getNotificationsFrequency(Context c) {
        if (PreferenceManager.getDefaultSharedPreferences(c).contains("notificationsFrequency")) {
            return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(c).getString("notificationsFrequency",
                    String.valueOf(DEFAULT_NOTIFICATIONS_FREQUENCY)));
        } else {
            return DEFAULT_NOTIFICATIONS_FREQUENCY;
        }
    }

    private static class Message {
        public final CharSequence title, message, ticker;
        public Message(Resources res, Activities events, Activities favoritings, Activities comments) {
            if (!favoritings.isEmpty() && comments.isEmpty()) {
                // only favoritings
                List<Track> tracks = favoritings.getUniqueTracks();
                ticker = res.getQuantityString(
                        R.plurals.dashboard_notifications_activity_ticker_like,
                        favoritings.size(),
                        favoritings.size());

                title = res.getQuantityString(
                        R.plurals.dashboard_notifications_activity_title_like,
                        favoritings.size(),
                        favoritings.size());

                if (tracks.size() == 1 && favoritings.size() == 1) {
                    message = res.getString(R.string.dashboard_notifications_activity_message_likes,
                            favoritings.get(0).getUser().username,
                            favoritings.get(0).getTrack().title);
                } else {
                    message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_like,
                            tracks.size(),
                            tracks.get(0).title,
                            (tracks.size() > 1 ? tracks.get(1).title : null));
                }
            } else if (favoritings.isEmpty() && !comments.isEmpty()) {
                // only comments
                List<Track> tracks = comments.getUniqueTracks();
                List<User> users = comments.getUniqueUsers();

                ticker = res.getQuantityString(
                        R.plurals.dashboard_notifications_activity_ticker_comment,
                        comments.size(),
                        comments.size());

                title = res.getQuantityString(
                        R.plurals.dashboard_notifications_activity_title_comment,
                        comments.size(),
                        comments.size());

                if (tracks.size() == 1) {
                    message = res.getQuantityString(
                            R.plurals.dashboard_notifications_activity_message_comment_single_track,
                            comments.size(),
                            comments.size(),
                            tracks.get(0).title,
                            comments.get(0).getUser().username,
                            comments.size() > 1 ? comments.get(1).getUser().username : null);
                } else {
                    message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_comment,
                                    users.size(),
                                    users.get(0).username,
                                    (users.size() > 1 ? users.get(1).username : null));
                }
            } else {
               // mix of favoritings and comments
                List<Track> tracks = events.getUniqueTracks();
                List<User> users = events.getUniqueUsers();
                ticker = res.getQuantityString(R.plurals.dashboard_notifications_activity_ticker_activity,
                        events.size(),
                        events.size());

                title = res.getQuantityString(R.plurals.dashboard_notifications_activity_title_activity,
                        events.size(),
                        events.size());

                message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_activity,
                        users.size(),
                        tracks.get(0).title,
                        users.get(0).username,
                        users.size() > 1 ? users.get(1).username : null);
            }
        }
    }
}