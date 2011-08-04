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
import java.util.Map;
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

            createDashboardNotification(app, ticker, title, message,"incoming");
        }
    }

    /* package */ static void syncOwn(SoundCloudApplication app, long lastSync)
            throws IOException {
        final int count = app.getAccountDataInt(User.DataKeys.NOTIFICATION_COUNT_OWN);

        Activities events = getOwnEvents(app, lastSync);
        if (!events.isEmpty() && events.size() > count) {
            app.setAccountData(User.DataKeys.NOTIFICATION_COUNT_OWN, events.size());

        }
    }

    public static boolean isIncomingEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsIncoming", true);
    }

    public static boolean isExclusiveEnabled(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("notificationsExclusive", true);
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
                                                    CharSequence message, String tab) {

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
        nm.notify(Consts.Notifications.DASHBOARD_NOTIFY_ID, n);
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