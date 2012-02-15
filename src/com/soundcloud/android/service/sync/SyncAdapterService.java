package com.soundcloud.android.service.sync;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.RemoteViews;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.c2dm.PushEvent;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.utils.CloudUtils;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;

import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SyncAdapterService extends Service {
    private static final String TAG = SyncAdapterService.class.getSimpleName();
    public static final String PREF_NOTIFICATIONS_FREQUENCY = "notificationsFrequency";
    public static final String PREF_LAST_SYNC_CLEANUP = "lastSyncCleanup";

    private ScSyncAdapter mSyncAdapter;

    public static final int NOTIFICATION_MAX = 100;
    public static final int MAX_ARTWORK_PREFETCH = 40; // only prefetch N amount of artwork links
    private static final String NOT_PLUS = (NOTIFICATION_MAX-1)+"+";

    private static final long DEFAULT_NOTIFICATIONS_FREQUENCY = 60*60*1000*4L; // 4h
    public static final long DEFAULT_SYNC_DELAY = 3600L;

    public static final long DEFAULT_STALE_TIME = 60*60*1000;         // 1 hr in ms
    public static final long ACTIVITY_STALE_TIME = DEFAULT_STALE_TIME;
    public static final long TRACK_STALE_TIME = DEFAULT_STALE_TIME;
    public static final long USER_STALE_TIME = DEFAULT_STALE_TIME * 12;  // users aren't as crucial
    public static final long CLEANUP_DELAY    = DEFAULT_STALE_TIME * 24; // every 24 hours

    public static final String EXTRA_CLEAR_MODE = "clearMode";
    public static final String EXTRA_PUSH_EVENT = "pushEvent";
    public static final String EXTRA_PUSH_EVENT_URI = "pushEventUri";

    public static final int CLEAR_ALL       = 1;
    public static final int REWIND_LAST_DAY = 2;

    enum SyncContent {
        MySounds(Content.ME_TRACKS, TRACK_STALE_TIME),
        MyFavorites(Content.ME_FAVORITES, TRACK_STALE_TIME),
        MyFollowings(Content.ME_FOLLOWINGS, USER_STALE_TIME),
        MyFollowers(Content.ME_FOLLOWERS, USER_STALE_TIME);

        SyncContent(Content content, long syncDelay) {
            this.content = content;
            this.syncDelay = syncDelay;
            this.prefSyncEnabledKey = "sync"+name();
        }

        public final Content content;
        public final long syncDelay;
        public final String prefSyncEnabledKey;

        public static List<Uri> configureSyncExtras(Context c, List<Uri> urisToSync, boolean force){
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
            for (SyncContent sc : SyncContent.values()){
                if (sp.getBoolean(sc.prefSyncEnabledKey, false)) {
                    final long lastUpdated = LocalCollection.getLastSync(sc.content.uri, c.getContentResolver());
                    if (System.currentTimeMillis() - lastUpdated > sc.syncDelay || force){
                        urisToSync.add(sc.content.uri);
                    }
                }
            }
            return urisToSync;
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
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "skipping sync because Wifi is diabled");
                }
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG,"Done with sync " + syncResult);
            }
        }
    }

    /** @noinspection UnusedParameters*/
    /* package */ static Intent performSync(final SoundCloudApplication app,
                                            Account account,
                                            Bundle extras,
                                            ContentProviderClient provider,
                                            final SyncResult syncResult) {
        if (!app.useAccount(account).valid()) {
            Log.w(TAG, "no valid token, skip sync");
            syncResult.stats.numAuthExceptions++;
            return null;
        }

        final boolean force = extras.getBoolean(ContentResolver.SYNC_EXTRAS_FORCE, false);
        final Intent intent = new Intent(app, ApiSyncService.class);
        final ArrayList<Uri> urisToSync = new ArrayList<Uri>();

        // for first sync set all last seen flags to "now"
        if (app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN) <= 0) {
            final long now = System.currentTimeMillis();
            app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, now);
            app.setAccountData(User.DataKeys.LAST_OWN_SEEN, now);
            app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, now);
        }

        PushEvent evt = PushEvent.fromExtras(extras);
        if (evt == PushEvent.FOLLOWER){
            if (PreferenceManager.getDefaultSharedPreferences(app).getBoolean("notificationsFollowers", true)
                    && extras.containsKey(SyncAdapterService.EXTRA_PUSH_EVENT_URI)){

                final Long id = getIdFromUri(Uri.parse(extras.getString(SyncAdapterService.EXTRA_PUSH_EVENT_URI)));
                if (id != -1){
                    User u = SoundCloudApplication.USER_CACHE.containsKey(id) ? SoundCloudApplication.USER_CACHE.get(id)
                            : SoundCloudDB.getUserById(app.getContentResolver(),id);
                    if (u != null && !u.isStale()){
                        showNewFollower(app, u);
                    } else {
                        try {
                            HttpResponse resp = app.get(Request.to(Endpoints.USERS + "/" + id));
                            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                                u = app.getMapper().readValue(resp.getEntity().getContent(), User.class);
                                SoundCloudDB.insertUser(app.getContentResolver(), u);
                                SoundCloudApplication.USER_CACHE.put(u);
                                showNewFollower(app, u);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            intent.setData(Content.ME_FOLLOWERS.uri);

        } else {
             if (shouldUpdateDashboard(app)) {
                if (isIncomingEnabled(app, extras)) urisToSync.add(Content.ME_SOUND_STREAM.uri);
                if (isExclusiveEnabled(app, extras)) urisToSync.add(Content.ME_EXCLUSIVE_STREAM.uri);
                if (isActivitySyncEnabled(app, extras)) urisToSync.add(Content.ME_ACTIVITIES.uri);
            }

            if (shouldSyncCollections(app)) {
                SyncContent.configureSyncExtras(app, urisToSync, force);
            }

            final long lastCleanup = PreferenceManager.getDefaultSharedPreferences(app).getLong(
                    PREF_LAST_SYNC_CLEANUP,
                    System.currentTimeMillis());

            if (System.currentTimeMillis() - lastCleanup > CLEANUP_DELAY || force) {
                urisToSync.add(Content.TRACK_CLEANUP.uri);
                urisToSync.add(Content.USERS_CLEANUP.uri);
            }

            intent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, urisToSync);
        }

        Looper.prepare();
        intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER,
                new ServiceResultReceiver(app, syncResult, extras));
        app.startService(intent);
        Looper.loop();
        return intent;
    }

    static class ServiceResultReceiver extends ResultReceiver {
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
                    if (shouldUpdateDashboard(app)) {
                        final long frequency = getNotificationsFrequency(app);
                        final long delta = System.currentTimeMillis() -
                                app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_AT);
                        if (delta > frequency) {
                            final long lastIncomingSeen = app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN);
                            final Activities incoming = !isIncomingEnabled(app, extras) ? Activities.EMPTY :
                                    Activities.getSince(Content.ME_SOUND_STREAM, app.getContentResolver(), lastIncomingSeen);

                            final Activities exclusive = !isExclusiveEnabled(app, extras) ? Activities.EMPTY
                                    : Activities.getSince(Content.ME_EXCLUSIVE_STREAM, app.getContentResolver(), lastIncomingSeen);

                            maybeNotifyIncoming(app, incoming, exclusive);
                        } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "skipping incoming notification, delta "+delta+" < frequency="+frequency);
                        }

                        final long lastOwnSeen = app.getAccountDataLong(User.DataKeys.LAST_OWN_SEEN);
                        final Activities news = !isActivitySyncEnabled(app, extras) ? Activities.EMPTY :
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
                    message = getExclusiveNotificationMessage(app, exclusive);
                    artwork_url = exclusive.getFirstAvailableArtwork();
                } else {
                    message = getIncomingNotificationMessage(app, incoming);
                }

                // either no exclusive or no exclusive artwork
                if (TextUtils.isEmpty(artwork_url)) {
                    artwork_url = incoming.getFirstAvailableArtwork();
                }

                if (incoming.newerThan(app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM)) ||
                        exclusive.newerThan(app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM))) {
                    prefetchArtwork(app, incoming, exclusive);

                    showDashboardNotification(app, ticker, title, message, createNotificationIntent(Actions.STREAM),
                            Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID, artwork_url);

                    app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, System.currentTimeMillis());
                    app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM,
                            Math.max(incoming.getTimestamp(), exclusive.getTimestamp()));

                    return true;
                } else return false;
            } else {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "no items, skip track notfication");
                return false;
            }
        }

        private boolean maybeNotifyOwn(SoundCloudApplication app, Activities activities, Bundle extras) {
            if (!activities.isEmpty()) {
                Activities favoritings = isLikeEnabled(app, extras) ? activities.favoritings() : Activities.EMPTY;
                Activities comments    = isCommentsEnabled(app, extras) ? activities.comments() : Activities.EMPTY;

                Message msg = new Message(app.getResources(), activities, favoritings, comments);

                if (activities.newerThan(app.getAccountDataLong(User.DataKeys.LAST_OWN_NOTIFIED_ITEM))) {
                    prefetchArtwork(app, activities);

                    showDashboardNotification(app, msg.ticker, msg.title, msg.message,
                            createNotificationIntent(Actions.ACTIVITY),
                            Consts.Notifications.DASHBOARD_NOTIFY_ACTIVITIES_ID,
                            activities.getFirstAvailableAvatar());

                    app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_ITEM, activities.getTimestamp());
                    return true;
                } else return false;
            } else return false;
        }

        private int prefetchArtwork(Context context, Activities... activities) {
            if (CloudUtils.isWifiConnected(context)) {
                Set<String> urls = new HashSet<String>();
                for (Activities a : activities) {
                    urls.addAll(a.artworkUrls());
                }
                int tofetch = MAX_ARTWORK_PREFETCH;
                for (String url : urls) {
                    ImageLoader.get(context).prefetch(url);
                    if (tofetch-- <= 0) break;
                }
                return Math.min(urls.size(), MAX_ARTWORK_PREFETCH);
            } else {
                // prefetch artwork only when connected to wifi
                return 0;
            }
        }
    }

    /* package */ static String getIncomingNotificationMessage(SoundCloudApplication app, Activities activites) {
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

    /* package */ static String getExclusiveNotificationMessage(SoundCloudApplication app, Activities activities) {
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

    private static void showNewFollower(SoundCloudApplication app, User u) {
        showDashboardNotification(app,
                app.getString(R.string.dashboard_notifications_ticker_follower),
                app.getString(R.string.dashboard_notifications_title_follower),
                app.getString(R.string.dashboard_notifications_message_follower, u.username),
                createNotificationIntent(Actions.USER_BROWSER).putExtra("user",u),
                Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID,
                u.avatar_url);
    }


    private static void showDashboardNotification(final Context context,
                                                  final CharSequence ticker,
                                                  final CharSequence title,
                                                  final CharSequence message,
                                                  final Intent intent,
                                                  final int id,
                                                  String artworkUri) {

        if (!SoundCloudApplication.useRichNotifications() || !ImageUtils.checkIconShouldLoad(artworkUri)) {
            showDashboardNotification(context, ticker, intent, title, message, id, null);
        } else {
            final Bitmap bmp = ImageLoader.get(context).getBitmap(artworkUri,null, new ImageLoader.Options(false));
            if (bmp != null){
                showDashboardNotification(context, ticker, intent, title, message, id, bmp);
            } else {
                ImageLoader.get(context).getBitmap(artworkUri,new ImageLoader.BitmapCallback(){
                    public void onImageLoaded(Bitmap loadedBmp, String uri) {
                        showDashboardNotification(context, ticker, intent, title, message, id, loadedBmp);
                    }
                    public void onImageError(String uri, Throwable error) {
                        showDashboardNotification(context, ticker, intent, title, message, id, null);
                    }
                });
            }
        }
    }



    private static void showDashboardNotification(Context context,
                                                  CharSequence ticker,
                                                  Intent intent,
                                                  CharSequence title,
                                                  CharSequence message,
                                                  int id,
                                                  Bitmap bmp) {

        final NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);


        final PendingIntent pi = PendingIntent.getActivity(context.getApplicationContext(), 0, intent, 0);

        final Notification n = new Notification(R.drawable.statusbar, ticker, System.currentTimeMillis());
        n.contentIntent = pi;
        n.flags = Notification.FLAG_AUTO_CANCEL;

        if (bmp == null){
            n.setLatestEventInfo(context.getApplicationContext(), title, message, pi);
        } else {
            final RemoteViews notificationView = new RemoteViews(context.getPackageName(), R.layout.dashboard_notification_v11);
                        notificationView.setTextViewText(R.id.title_txt, title);
                        notificationView.setTextViewText(R.id.content_txt, message);

            notificationView.setImageViewBitmap(R.id.icon,bmp);
            n.contentView = notificationView;
        }
        nm.notify(id, n);
    }


    // only used for debugging
    public static void requestNewSync(SoundCloudApplication app, int clearMode) {
        switch (clearMode) {
            case CLEAR_ALL:
                app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1);
                app.setAccountData(User.DataKeys.LAST_OWN_SEEN, 1);
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_ITEM, 1);
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM, 1);
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, 1);
                clearActivities(app.getContentResolver());
                break;
            case REWIND_LAST_DAY:
                final long rewindTime = 24 * 3600000L; // 1d
                rewind(app, User.DataKeys.LAST_INCOMING_SEEN, null, rewindTime);
                rewind(app, User.DataKeys.LAST_OWN_SEEN, null, rewindTime);
                rewind(app, User.DataKeys.LAST_OWN_NOTIFIED_ITEM, null,  rewindTime);
                rewind(app, User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM, null,  rewindTime);
                rewind(app, User.DataKeys.LAST_INCOMING_NOTIFIED_AT, null, rewindTime);
                clearActivities(app.getContentResolver());
                break;
            default:
        }

        final Bundle extras = new Bundle();
        extras.putInt(EXTRA_CLEAR_MODE, clearMode);
        ContentResolver.requestSync(app.getAccount(), ScContentProvider.AUTHORITY, extras);
    }

    private static Intent createNotificationIntent(String action){
        return new Intent(action)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    private static void clearActivities(ContentResolver resolver){
        // drop all activities before re-sync
        int deleted = Activities.clear(null, resolver);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "deleted "+deleted+ " activities");
        }
    }

    private static void rewind(SoundCloudApplication app, String key1, String key2, long amount) {
        app.setAccountData(key1, app.getAccountDataLong(key2 == null ? key1 : key2) - amount);
    }

    private static long getNotificationsFrequency(Context c) {
        if (PreferenceManager.getDefaultSharedPreferences(c).contains(PREF_NOTIFICATIONS_FREQUENCY)) {
            return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(c).getString(PREF_NOTIFICATIONS_FREQUENCY,
                    String.valueOf(DEFAULT_NOTIFICATIONS_FREQUENCY)));
        } else {
            return DEFAULT_NOTIFICATIONS_FREQUENCY;
        }
    }

    private static class Message {
        public final CharSequence title, message, ticker;
        public Message(Resources res, Activities activities, Activities favoritings, Activities comments) {
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
                List<Track> tracks = activities.getUniqueTracks();
                List<User> users = activities.getUniqueUsers();
                ticker = res.getQuantityString(R.plurals.dashboard_notifications_activity_ticker_activity,
                        activities.size(),
                        activities.size());

                title = res.getQuantityString(R.plurals.dashboard_notifications_activity_title_activity,
                        activities.size(),
                        activities.size());

                message = res.getQuantityString(R.plurals.dashboard_notifications_activity_message_activity,
                        users.size(),
                        tracks.get(0).title,
                        users.get(0).username,
                        users.size() > 1 ? users.get(1).username : null);
            }
        }
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

    private static boolean isIncomingEnabled(Context c, Bundle extras) {
        PushEvent evt = PushEvent.fromExtras(extras);
        return PreferenceManager
                .getDefaultSharedPreferences(c)
                .getBoolean("notificationsIncoming", true) && evt == PushEvent.NULL;
    }

    private static boolean isExclusiveEnabled(Context c, Bundle extras) {
        PushEvent evt = PushEvent.fromExtras(extras);
        return PreferenceManager
                .getDefaultSharedPreferences(c)
                .getBoolean("notificationsExclusive", true) && evt == PushEvent.NULL;
    }

    private static boolean isLikeEnabled(Context c, Bundle extras) {
        PushEvent evt = PushEvent.fromExtras(extras);
        return PreferenceManager
                .getDefaultSharedPreferences(c)
                .getBoolean("notificationsFavoritings", true) && (evt == PushEvent.NULL || evt == PushEvent.LIKE);
    }

    private static boolean isActivitySyncEnabled(Context c, Bundle extras) {
        return isLikeEnabled(c, extras) || isCommentsEnabled(c, extras);
    }

    private static boolean isCommentsEnabled(Context c, Bundle extras) {
        PushEvent evt = PushEvent.fromExtras(extras);
        return PreferenceManager
                .getDefaultSharedPreferences(c)
                .getBoolean("notificationsComments", true) && (evt == PushEvent.NULL || evt == PushEvent.COMMENT);
    }

    private static boolean isSyncWifiOnlyEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("syncWifiOnly", true);
    }

    private static long getIdFromUri(Uri uri) {
        if (uri != null && "soundcloud".equalsIgnoreCase(uri.getScheme())) {
            final String specific = uri.getSchemeSpecificPart();
            final String[] components = specific.split(":", 2);
            if (components != null && components.length == 2) {
                final String type = components[0];
                final String id = components[1];
                if (type != null && id != null) {
                    try {
                        return Long.parseLong(id);
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
        return -1;
    }
}
