package com.soundcloud.android.service.sync;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

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
import android.content.SyncResult;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SyncAdapterService extends Service {
    private static final String TAG = SoundCloudApplication.class.getSimpleName();
    private ScSyncAdapter mSyncAdapter;

    public static final int NOTIFICATION_MAX = 100;
    public static final String NOT_PLUS = (NOTIFICATION_MAX-1)+"+";
    private static final long DEFAULT_POLL_FREQUENCY = 14400; //60*60*4

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
        private SoundCloudApplication mApp;

        public ScSyncAdapter(SoundCloudApplication app) {
            super(app, true);
            mApp = app;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                                  ContentProviderClient provider, SyncResult syncResult) {

            if (SoundCloudApplication.DEV_MODE) {
                Log.d(TAG, "onPerformSync("+account+","+extras+","+authority+","+provider+","+syncResult+")");
            }
            if (shouldSync()) {
                SyncAdapterService.performSync(mApp, account, extras, provider, syncResult);
            } else {
                Log.d(TAG, "skipping sync because Wifi is diabled");
            }
        }

        private boolean shouldSync() {
            if (isWifiOnlyEnabled(mApp)) {
                ConnectivityManager mgr = (ConnectivityManager) mApp.getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo ni = mgr == null ? null : mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                return ni != null && ni.isConnectedOrConnecting();
            } else {
                return true;
            }
        }
    }

    /** @noinspection UnusedParameters*/
    /* package */ static void performSync(final SoundCloudApplication app,
                                    Account account,
                                    Bundle extras,
                                    ContentProviderClient provider,
                                    final SyncResult syncResult) {
        if (app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN) <= 0) {
            final long now = System.currentTimeMillis();
            app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, now);
            app.setAccountData(User.DataKeys.LAST_OWN_SEEN, now);
        } else {

            if (app.useAccount(account).valid()) {
                // TODO, do not sync everything unless it is set that way in prefs
                Looper.prepare();
                final Intent intent = new Intent(app, ApiSyncService.class);
                ArrayList<String> urisToSync = new ArrayList<String>();

                if (isIncomingEnabled(app))     urisToSync.add(Content.ME_ACTIVITIES.uri.toString());
                if (isExclusiveEnabled(app))    urisToSync.add(Content.ME_EXCLUSIVE_STREAM.uri.toString());
                if (isActivitySyncEnabled(app)) urisToSync.add(Content.ME_ACTIVITIES.uri.toString());

                urisToSync.add(Content.TRACK_CLEANUP.uri.toString());
                urisToSync.add(Content.USERS_CLEANUP.uri.toString());

                intent.putStringArrayListExtra("syncUris", urisToSync);
                intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        switch (resultCode) {
                            case ApiSyncService.STATUS_RUNNING: {
                                break;
                            }
                            case ApiSyncService.STATUS_ERROR: {
                                SyncResult serviceResult = resultData.getParcelable(ApiSyncService.EXTRA_SYNC_RESULT);
                                syncResult.stats.numAuthExceptions = serviceResult.stats.numAuthExceptions;
                                syncResult.stats.numIoExceptions = serviceResult.stats.numIoExceptions;
                                Looper.myLooper().quit();
                                break;
                            }
                            case ApiSyncService.STATUS_FINISHED: {

                                try {

                                    final long lastIncomingSeen = app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN);
                                    final Activities incoming = !isIncomingEnabled(app) ? Activities.EMPTY
                                            : Activities.fromJSON(ActivitiesCache.getCacheFile(app, Request.to(Endpoints.MY_ACTIVITIES)));

                                    final Activities exclusive = !isExclusiveEnabled(app) ? Activities.EMPTY
                                            : Activities.fromJSON(ActivitiesCache.getCacheFile(app, Request.to(Endpoints.MY_EXCLUSIVE_TRACKS)));

                                    syncIncoming(app, incoming.filter(lastIncomingSeen), exclusive.filter(lastIncomingSeen));

                                    final Activities news = !isActivitySyncEnabled(app) ? Activities.EMPTY
                                            : Activities.fromJSON(ActivitiesCache.getCacheFile(app, Request.to(Endpoints.MY_NEWS)));

                                    syncOwn(app, news.filter(app.getAccountDataLong(User.DataKeys.LAST_OWN_SEEN)));

                                } catch (IOException e) {
                                    e.printStackTrace();
                                    syncResult.stats.numIoExceptions++;
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

    }

    /* package */ static void syncIncoming(SoundCloudApplication app, Activities incoming, Activities exclusive)
            throws IOException {

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


            if (incoming.newerThan(app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED)) ||
                exclusive.newerThan(app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED))) {

                createDashboardNotification(app, ticker, title, message,
                    Actions.STREAM,
                    Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID);

                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED,
                        Math.max(incoming.getTimestamp(), exclusive.getTimestamp()));
            }
        }
    }
    static Activities getOwnEvents(SoundCloudApplication app, Account account) throws IOException {
        return ActivitiesCache.get(app, account, Request.to(Endpoints.MY_NEWS));
    }

    /* package */ static void syncOwn(SoundCloudApplication app, Activities events)
            throws IOException {



        if (!events.isEmpty()) {
            Activities favoritings = isFavoritingEnabled(app) ? events.favoritings() : Activities.EMPTY;
            Activities comments    = isCommentsEnabled(app) ? events.comments() : Activities.EMPTY;

            Message msg = new Message(app.getResources(), events, favoritings, comments);

            if (events.newerThan(app.getAccountDataLong(User.DataKeys.LAST_OWN_NOTIFIED))) {
                createDashboardNotification(app, msg.ticker, msg.title, msg.message, Actions.ACTIVITY,
                    Consts.Notifications.DASHBOARD_NOTIFY_ACTIVITIES_ID);

                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED, events.getTimestamp());
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
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED, 1);
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED, 1);
                ActivitiesCache.clear(app);
                break;
            case 1:
                app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN) - (24 * 3600000));
                app.setAccountData(User.DataKeys.LAST_OWN_SEEN, app.getAccountDataLong(User.DataKeys.LAST_OWN_SEEN) - (24 * 3600000));
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED, app.getAccountDataLong(User.DataKeys.LAST_OWN_NOTIFIED) - (24 * 3600000));
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED, app.getAccountDataLong(User.DataKeys.LAST_INCOMING_NOTIFIED) - (24 * 3600000));
                ActivitiesCache.clear(app);
                break;
        }
        ContentResolver.requestSync(app.getAccount(), ScContentProvider.AUTHORITY, new Bundle());
    }

    public static boolean isWifiOnlyEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsWifiOnly", false);
    }

    public static boolean isIncomingEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsIncoming", true);
    }

    public static boolean isExclusiveEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsExclusive", true);
    }

    public static boolean isFavoritingEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsFavoritings", true);
    }

    public static boolean isActivitySyncEnabled(Context c) {
        return isFavoritingEnabled(c) || isCommentsEnabled(c);
    }

    public static boolean isCommentsEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsComments", true);
    }

    public static long getDefaultNotificationsFrequency(Context c) {
        if (PreferenceManager.getDefaultSharedPreferences(c).contains("notificationsFrequency")) {
            return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(c).getString("notificationsFrequency",
                    String.valueOf(DEFAULT_POLL_FREQUENCY)));
        } else {
            return DEFAULT_POLL_FREQUENCY;
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