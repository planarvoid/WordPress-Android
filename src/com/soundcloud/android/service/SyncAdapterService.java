package com.soundcloud.android.service;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
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
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SyncAdapterService extends Service {
    private static final String TAG = "ScSyncAdapterService";
    private ScSyncAdapter mSyncAdapter;

    private static final int NOTIFICATION_MAX = 100;
    private static final String NOT_PLUS = (NOTIFICATION_MAX-1)+"+";

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
            try {
                SyncAdapterService.performSync(mApp, account, extras, provider, syncResult);
            } catch (OperationCanceledException e) {
                Log.w(TAG, "canceled", e);
            }
        }
    }

    /** @noinspection UnusedParameters*/
    /* package */ static void performSync(final SoundCloudApplication app,
                                    Account account,
                                    Bundle extras,
                                    ContentProviderClient provider,
                                    SyncResult syncResult)
            throws OperationCanceledException {

        // for initial sync, don't bother telling them about their entire dashboard
        if (app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN) == 0) {
            app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, System.currentTimeMillis());
        } else {
            app.useAccount(account);

            // how many have they already been notified about, don't create repeat notifications for no new tracks
            try {
                syncIncoming(app, app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN));
                syncOwn(app, app.getAccountDataLong(User.DataKeys.LAST_OWN_SEEN));
            } catch (IOException e) {
                Log.w(TAG, "i/o", e);
            }
        }
    }

    /* package */ static void syncIncoming(SoundCloudApplication app, long lastSync)
            throws IOException {
        final int count = app.getAccountDataInt(User.DataKeys.NOTIFICATION_COUNT_INCOMING);

        Activities incomingEvents = getNewIncomingEvents(app, lastSync);
        Activities exclusiveEvents = getNewExclusiveEvents(app, lastSync);

        Set<Long> ids = new HashSet<Long>(incomingEvents.size());
        for (Event e : incomingEvents) ids.add(e.origin_id);
        for (Event e : exclusiveEvents) ids.add(e.origin_id);
        final int totalUnseen = ids.size();

        final boolean hasIncoming  = !incomingEvents.isEmpty();
        final boolean hasExclusive = !exclusiveEvents.isEmpty();
        final boolean showNotification = totalUnseen > count;
        if ((hasIncoming || hasExclusive) && showNotification) {
            final CharSequence title, message, ticker;
            app.setAccountData(User.DataKeys.NOTIFICATION_COUNT_INCOMING, totalUnseen);

            if (totalUnseen == 1) {
                ticker = app.getString(
                        R.string.dashboard_notifications_ticker_single);
                title = app.getString(
                        R.string.dashboard_notifications_title_single);
            } else {
                ticker = String.format(app.getString(
                        R.string.dashboard_notifications_ticker), totalUnseen >= NOTIFICATION_MAX ? NOT_PLUS : totalUnseen);

                title = String.format(app.getString(
                        R.string.dashboard_notifications_title), totalUnseen >= NOTIFICATION_MAX ? NOT_PLUS : totalUnseen);
            }

            if (hasExclusive) {
                message = getExclusiveMessaging(app, exclusiveEvents);
            } else {
                message = getIncomingMessaging(app, incomingEvents);
            }

            createDashboardNotification(app, ticker, title, message,
                    "incoming",
                    Consts.Notifications.DASHBOARD_NOTIFY_STREAM_ID);
        }
    }

    /* package */ static void syncOwn(SoundCloudApplication app, long lastSync)
            throws IOException {
        final int count = app.getAccountDataInt(User.DataKeys.NOTIFICATION_COUNT_OWN);

        Activities events = getOwnEvents(app, lastSync);
        if (!events.isEmpty() && events.size() > count) {
            app.setAccountData(User.DataKeys.NOTIFICATION_COUNT_OWN, events.size());
            final CharSequence title, message, ticker;

            Activities favoritings = isFavoritingEnabled(app) ? events.favoritings() : Activities.EMPTY;
            Activities comments    = isCommentsEnabled(app) ? events.comments() : Activities.EMPTY;

            if (!favoritings.isEmpty() && comments.isEmpty()) {
                // only favoritings
                List<Track> tracks = favoritings.getUniqueTracks();

                if (favoritings.size() == 1) {
                    ticker = "New like";
                    title = "A new like";
                } else {
                    title = ticker = favoritings.size()+ " new likes";
                }

                if (tracks.size() == 1) {
                    if (favoritings.size() == 1) {
                        message = favoritings.get(0).user.username + " likes " + favoritings.get(0).track.title;
                    } else {
                        message = "on "+tracks.get(0).title;
                    }
                } else if (tracks.size() == 2) {
                   message = "on "+tracks.get(0).title + " and " +tracks.get(1).title;
                } else {
                   message = "on "  +tracks.get(0).title + ", " +tracks.get(1).title + " and other sounds";
                }
            } else if (favoritings.isEmpty() && !comments.isEmpty()) {
                // only comments
                List<Track> tracks = comments.getUniqueTracks();
                List<User> users = comments.getUniqueUsers();

                if (comments.size() == 1) {
                    title = ticker = "1 new comment";
                } else {
                    title = ticker =  comments.size()+" new comments";
                }

                if (tracks.size() == 1) {
                    if (comments.size() == 1) {
                        message = "new comment on "+comments.get(0).track.title+" from "+comments.get(0).user.username;
                    } else if (comments.size() == 2) {
                        message = comments.size() + " new comments on "+comments.get(0).track.title+" from "+comments.get(0).user.username+
                        " and "+comments.get(1).user.username;
                    } else {
                        message = comments.size() + " new comments on "+comments.get(0).track.title+" from "+comments.get(0).user.username+
                        ", "+comments.get(1).user.username+ " and others";
                    }
                } else if (users.size() == 2) {
                        message = "Comments from "+users.get(0).username+" and "+users.get(1).username;
                } else {
                        message = "Comments from "+users.get(0).username+", "+users.get(1).username
                                +" and others";
                }
            } else {
               // mix of favoritings and comments
                List<Track> tracks = events.getUniqueTracks();
                List<User> users = events.getUniqueUsers();

                ticker = title = events.size() + " new activities";

                if (users.size() == 1 && tracks.size() == 1) {
                    message = "from "+users.get(0).username+" on "+tracks.get(0).title;
                } else if (users.size() == 2 && tracks.size() > 1) {
                    message = "Comments and likes from "+users.get(0).username+ " and "+users.get(1).username;
                } else {
                    message = "Comments and likes from "+users.get(0).username+ ", "+users.get(1).username+
                        " and others";
                }
            }
            createDashboardNotification(app, ticker, title, message, "activity",
                    Consts.Notifications.DASHBOARD_NOTIFY_ACTIVITIES_ID);
        }
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

    public static boolean isCommentsEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsComments", true);
    }

    private static Activities getNewIncomingEvents(SoundCloudApplication app, long since) throws IOException {
        return getNewIncomingEvents(app, since, false);
    }

    private static Activities getNewExclusiveEvents(SoundCloudApplication app, long since) throws IOException {
        return getNewIncomingEvents(app, since, true);
    }

    /* package */ static Activities getNewIncomingEvents(SoundCloudApplication app, long since, boolean exclusive)
            throws IOException {
        if ((!exclusive && !isIncomingEnabled(app)) || (exclusive && !isExclusiveEnabled(app))) {
            return Activities.EMPTY;
        } else {
            return getEvents(app, since, exclusive ? Endpoints.MY_EXCLUSIVE_TRACKS : Endpoints.MY_ACTIVITIES);
        }
    }

    /* package */ static Activities getOwnEvents(SoundCloudApplication application, long since)
            throws IOException {
        return getEvents(application, since, AndroidCloudAPI.MY_NEWS);
    }

    /* package */
    static Activities getEvents(SoundCloudApplication app, final long since, final String resource)
            throws IOException {
        boolean caughtUp = false;
        Activities activities = null;
        List<Event> events = new ArrayList<Event>();
        do {
            Request request = Request.to(resource).add("limit", 20);
            if (activities != null) {
                request.add("cursor", activities.getCursor());
            }

            HttpResponse response = app.get(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                activities = app.getMapper().readValue(response.getEntity().getContent(), Activities.class);
                if (activities.includes(since)) {
                    caughtUp = true; // nothing new
                } else {
                    for (Event evt : activities) {
                        if (evt.created_at.getTime() <= since) {
                            caughtUp = true;
                            break;
                        } else {
                            events.add(evt);
                        }
                    }
                }
            } else {
                Log.w(TAG, "unexpected status code: " + response.getStatusLine());
                throw new IOException(response.getStatusLine().toString());
            }
        } while (!caughtUp
                && events.size() < NOTIFICATION_MAX
                && !TextUtils.isEmpty(activities.next_href));

        return new Activities(events);
    }


    /* package */ static String getIncomingMessaging(SoundCloudApplication app, Activities activites) {
        List<User> users = activites.getUniqueUsers();
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
                    activities.get(0).track.user.username);

        } else {
            List<User> users = activities.getUniqueUsers();
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

    private static void createDashboardNotification(SoundCloudApplication app,
                                                    CharSequence ticker,
                                                    CharSequence title,
                                                    CharSequence message, String tab, int id) {

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) app.getSystemService(ns);

        Intent intent = (new Intent(app, Main.class))
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("tabTag", tab);

        PendingIntent pi = PendingIntent.getActivity(app.getApplicationContext(),
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification(R.drawable.statusbar, ticker, System.currentTimeMillis());
        n.contentIntent = pi;
        n.flags = Notification.FLAG_AUTO_CANCEL;
        n.setLatestEventInfo(app.getApplicationContext(), title, message, pi);
        nm.notify(id, n);
    }

    // only used for debugging
    public static void requestNewSync(SoundCloudApplication app) {
        app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1);
        app.setAccountData(User.DataKeys.LAST_OWN_SEEN, 1);

        app.setAccountData(User.DataKeys.NOTIFICATION_COUNT_INCOMING, null);
        app.setAccountData(User.DataKeys.NOTIFICATION_COUNT_OWN, null);
        ContentResolver.requestSync(app.getAccount(), ScContentProvider.AUTHORITY, new Bundle());
    }
}