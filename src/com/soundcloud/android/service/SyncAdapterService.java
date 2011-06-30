package com.soundcloud.android.service;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.*;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DatabaseHelper;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.james.mime4j.io.MaxHeaderLimitException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class SyncAdapterService extends Service {
    private static final String TAG = "ScSyncAdapterService";
    private ScSyncAdapter mSyncAdapter;
    private static final int NOTIFICATION_MAX = 100;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
        mSyncAdapter = new ScSyncAdapter((SoundCloudApplication) getApplication(), this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mSyncAdapter.getSyncAdapterBinder();
    }

    public static class ScSyncAdapter extends AbstractThreadedSyncAdapter {
        private Context mContext;
        private SoundCloudApplication mApp;

        public ScSyncAdapter(SoundCloudApplication app, Context context) {
            super(context, true);
            mContext = context;
            mApp = app;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                                  ContentProviderClient provider, SyncResult syncResult) {
            Log.d(TAG, "onPerformSync("+account+","+extras+","+authority+","+provider+","+syncResult+")");
            try {
                SyncAdapterService.performSync(mApp, mContext, account, extras, authority,
                        provider, syncResult);
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            }


        }
    }


    private static void performSync(final SoundCloudApplication app, Context context, Account account, Bundle extras,
                                    String authority, ContentProviderClient provider, SyncResult syncResult)
            throws OperationCanceledException {

        app.useAccount(account);

        try {
            ArrayList<Event> incomingEvents = getNewIncomingEvents(app,
                    app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SYNC_EVENT_TIMESTAMP), false);

            ArrayList<Event> incomingExclusive = getNewIncomingEvents(app,
                    app.getAccountDataLong(User.DataKeys.LAST_EXCLUSIVE_SYNC_EVENT_TIMESTAMP), true);

            Log.i("SyncAdapterService","Got incoming " + incomingEvents.size());
            Log.i("SyncAdapterService","Got exclusives " + incomingExclusive.size());

            if (incomingEvents.size() > 0 || incomingExclusive.size() > 0) {

                if (incomingEvents.size() > 0 ) {
                    app.setAccountData(User.DataKeys.LAST_INCOMING_SYNC_EVENT_TIMESTAMP, incomingEvents.get(0).created_at.getTime());
                }

                if (incomingExclusive.size() > 0 ) {
                    app.setAccountData(User.DataKeys.LAST_EXCLUSIVE_SYNC_EVENT_TIMESTAMP, incomingExclusive.get(0).created_at.getTime());
                }

                int incomingUnseen = app.getAccountDataInt(User.DataKeys.CURRENT_INCOMING_UNSEEN) + incomingEvents.size();
                int exclusiveUnseen = app.getAccountDataInt(User.DataKeys.CURRENT_EXCLUSIVE_UNSEEN) + incomingExclusive.size();

                app.setAccountData(User.DataKeys.CURRENT_INCOMING_UNSEEN, incomingUnseen);
                app.setAccountData(User.DataKeys.CURRENT_EXCLUSIVE_UNSEEN, exclusiveUnseen);

                CharSequence title,message,ticker;

                if (incomingUnseen == 1) {
                    ticker = app.getApplicationContext().getString(
                            R.string.dashboard_notifications_ticker_single);
                    title = app.getApplicationContext().getString(
                            R.string.dashboard_notifications_title_single);
                } else {
                    ticker = String.format(app.getApplicationContext().getString(
                            R.string.dashboard_notifications_ticker), incomingUnseen > 99 ? "99+" : incomingUnseen);

                    title = String.format(app.getApplicationContext().getString(
                            R.string.dashboard_notifications_title), incomingUnseen > 99 ? "99+" : incomingUnseen);
                }

                if (exclusiveUnseen > 0) {
                    message = getExclusiveMessaging(app, incomingExclusive);
                } else {
                    message = getIncomingMessaging(app, incomingEvents);
                }
                createDashboardNotification(app, ticker, title, message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<Event> getNewIncomingEvents(SoundCloudApplication app, long activitiesSince, boolean exclusive) throws IOException {

        boolean caughtUp = false;
        Activities activities = null;
        ArrayList<Event> incomingEvents = new ArrayList<Event>();

        do {
            Request request = Request.to(exclusive ? Endpoints.MY_EXCLUSIVE_TRACKS : Endpoints.MY_ACTIVITIES).add("limit", 20);
            if (activities != null) {request.add("cursor", activities.getCursor());}

            HttpResponse response = app.get(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                activities = app.getMapper().readValue(response.getEntity().getContent(), Activities.class);

                if (activities.collection.size() > 0 && activities.collection.get(0).created_at.getTime() > activitiesSince) {
                    for (Event evt : activities) {
                        if (evt.created_at.getTime() <= activitiesSince) {
                            caughtUp = true;
                            break;
                        }
                        incomingEvents.add(evt);
                    }
                }
            }
        } while (!caughtUp && incomingEvents.size() < NOTIFICATION_MAX && !TextUtils.isEmpty(activities.next_href));

        return incomingEvents;
    }

    private static String getIncomingMessaging(SoundCloudApplication app, ArrayList<Event> events) {

        ArrayList<User> users = getUniqueUsersFromEvents(events);
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

    private static String getExclusiveMessaging(SoundCloudApplication app, ArrayList<Event> events) {


        if (events.size() == 1) {
            return String.format(
                    app.getString(R.string.dashboard_notifications_message_single_exclusive),
                    events.get(0).getTrack().user.username);

        } else {
            ArrayList<User> users = getUniqueUsersFromEvents(events);
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

    private static ArrayList<User> getUniqueUsersFromEvents(ArrayList<Event> events){
        ArrayList<User> users = new ArrayList<User>();
        for (Event e : events){
            Log.i("asdf","checking event " + e.getTrack());
                boolean found = false;
                for (User u : users){
                    if (u.id == e.getTrack().user.id){
                        found = true;
                        break;
                    }
                }
                if (!found) users.add(e.getTrack().user);
        }
        return users;
    }

    private static void createDashboardNotification(SoundCloudApplication app, CharSequence ticker, CharSequence title, CharSequence message) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) app
                .getSystemService(ns);

        Intent i = (new Intent(app, Main.class))
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("tabTag", "incoming");

        PendingIntent pi = PendingIntent.getActivity(app.getApplicationContext(),
                0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        int icon = R.drawable.statusbar;
        Notification notification = new Notification(icon, ticker, System
                .currentTimeMillis());
        notification.contentIntent = pi;
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        notification.setLatestEventInfo(app.getApplicationContext(), title,
                message, pi);
        mNotificationManager.notify(Consts.Notifications.DASHBOARD_NOTIFY_ID, notification);
    }
}