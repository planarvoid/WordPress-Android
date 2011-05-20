package com.soundcloud.android.service;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.objects.User;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

public class SyncAdapterService extends Service {
    private static final String TAG = "ScSyncAdapterService";
    private ScSyncAdapter mSyncAdapter;

    private static ContentResolver mContentResolver;
    public static final int DASHBOARD_NOTIFICATION_ID = R.layout.player_touch_bar;

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
            // pre SDK 8 doesn't allow auto syncing, so with our current list loading UI
            // it is easier to just enabling remote pulling only for now
            Log.d(TAG, "onPerformSync("+account+","+extras+","+authority+","+provider+","+syncResult+")");

            /*
            if (Build.VERSION.SDK_INT >= 8) {
                try {
                    SyncAdapterService.performSync(mApp, mContext, account, extras, authority,
                            provider, syncResult);
                } catch (OperationCanceledException ignored) {
                }
            }
            */
        }
    }


    private static void performSync(final SoundCloudApplication app, Context context, Account account, Bundle extras,
                                    String authority, ContentProviderClient provider, SyncResult syncResult)
            throws OperationCanceledException {
        mContentResolver = context.getContentResolver();


        app.useAccount(account);

        int incomingUnseen = app.getAccountDataInt(User.DataKeys.CURRENT_INCOMING_UNSEEN);
        int exclusiveUnseen = app.getAccountDataInt(User.DataKeys.CURRENT_EXCLUSIVE_UNSEEN);

        int newIncoming = 0;
        int newExclusive = 0;

        final long user_id = app.getAccountDataLong(User.DataKeys.USER_ID);
        try {
            if (app.lockUpdateRecentIncoming(false)) {
                newIncoming = SoundCloudDB.updateActivities(app, mContentResolver, user_id, false);
                app.unlockUpdateRecentIncoming(false);
            }
            if (app.lockUpdateRecentIncoming(true)) {
                newExclusive = SoundCloudDB.updateActivities(app, mContentResolver, user_id, true);
                app.unlockUpdateRecentIncoming(true);
            }
        } catch (JsonParseException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "error during sync", e);
        } catch (JsonMappingException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "error during sync", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "error during sync", e);
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            Log.e(TAG, "error during sync", e);
        }

        app.setAccountData(User.DataKeys.CURRENT_INCOMING_UNSEEN, incomingUnseen + newIncoming);
        app.setAccountData(User.DataKeys.CURRENT_EXCLUSIVE_UNSEEN, exclusiveUnseen + newExclusive);

        Intent intent = new Intent();
        intent.setAction(Dashboard.SYNC_CHECK_ACTION);
        app.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (getResultCode() == Activity.RESULT_CANCELED) { // Activity caught it

                            int maxStored = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
                                    app).getString("dashboardMaxStored", "100"));

                            SoundCloudDB.cleanStaleActivities(mContentResolver, user_id,
                                    maxStored, true);
                            SoundCloudDB.cleanStaleActivities(mContentResolver, user_id,
                                    maxStored, false);

                            CharSequence title;
                            CharSequence message = "";
                            CharSequence ticker;
                            boolean gotoExclusive = false;

                            int exclusiveUnseen = app.getAccountDataInt(User.DataKeys.CURRENT_EXCLUSIVE_UNSEEN);
                            int incomingUnseen = app.getAccountDataInt(User.DataKeys.CURRENT_INCOMING_UNSEEN);

                            if (exclusiveUnseen + incomingUnseen == 0) {
                                return;
                            } else if (incomingUnseen == 1) {
                                ticker = app.getApplicationContext().getString(
                                        R.string.dashboard_notifications_ticker_single);
                                title = app.getApplicationContext().getString(
                                        R.string.dashboard_notifications_title_single);
                            } else {
                                ticker = String.format(app.getApplicationContext().getString(
                                        R.string.dashboard_notifications_ticker), incomingUnseen);

                                title = String.format(app.getApplicationContext().getString(
                                        R.string.dashboard_notifications_title), incomingUnseen);
                            }

                            User[] fromUsers;

                            // ensure valid exclusive users
                            if (exclusiveUnseen > 0) {
                                fromUsers = SoundCloudDB.getUsersFromRecentActivities(
                                        mContentResolver, user_id, true, exclusiveUnseen);

                                if (fromUsers.length == 0) {
                                    // database was cleared but the unseen counter wasn't
                                    exclusiveUnseen = 0;
                                    app.setAccountData(User.DataKeys.CURRENT_INCOMING_UNSEEN, 0);
                                } else {
                                    message = getExclusiveMessaging(app, fromUsers, exclusiveUnseen);
                                }
                            }

                            if (exclusiveUnseen == 0) {
                                fromUsers = SoundCloudDB.getUsersFromRecentActivities(
                                        mContentResolver, user_id, false, incomingUnseen);

                                if (fromUsers.length == 0) {
                                    // database was cleared but the unseen counter wasn't
                                    incomingUnseen = 0;
                                    app.setAccountData(User.DataKeys.CURRENT_INCOMING_UNSEEN, 0);
                                } else {
                                    message = getIncomingMessaging(app, fromUsers);
                                }

                            }

                            if (message != "") createDashboardNotification(app, ticker, title, message, gotoExclusive);

                        }
                    }
                }, null, Activity.RESULT_CANCELED, null, null);

    }

    private static String getIncomingMessaging(SoundCloudApplication app, User[] fromUsers) {
        switch (fromUsers.length) {
            case 1:
                return String.format(
                        app.getString(R.string.dashboard_notifications_message_incoming),
                        fromUsers[0].username);
            case 2:
                return String.format(
                        app.getString(R.string.dashboard_notifications_message_incoming_2),
                        fromUsers[0].username, fromUsers[1].username);
            default:
                return String.format(
                        app.getString(R.string.dashboard_notifications_message_incoming_others),
                        fromUsers[0].username, fromUsers[1].username);

        }
    }

    private static String getExclusiveMessaging(SoundCloudApplication app, User[] fromUsers,
                                                int exclusiveUnseen) {
        if (exclusiveUnseen == 1) {
            return String.format(
                    app.getString(R.string.dashboard_notifications_message_single_exclusive),
                    fromUsers[0].username);

        } else
            switch (fromUsers.length) {
                case 1:
                    return String.format(
                            app.getString(R.string.dashboard_notifications_message_exclusive),
                            fromUsers[0].username);
                case 2:
                    return String.format(
                            app.getString(R.string.dashboard_notifications_message_exclusive_2),
                            fromUsers[0].username, fromUsers[1].username);
                default:
                    return String.format(app
                            .getString(R.string.dashboard_notifications_message_exclusive_others),
                            fromUsers[0].username, fromUsers[1].username);
            }
    }

    private static void createDashboardNotification(SoundCloudApplication app, CharSequence ticker, CharSequence title, CharSequence message, boolean exclusive) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) app
                .getSystemService(ns);

        Intent i = (new Intent(app, Main.class))
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("tabTag", (exclusive ? "exclusive" : "incoming"));

        PendingIntent pi = PendingIntent.getActivity(app.getApplicationContext(),
                0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        int icon = R.drawable.statusbar;
        Notification notification = new Notification(icon, ticker, System
                .currentTimeMillis());
        notification.contentIntent = pi;
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        notification.setLatestEventInfo(app.getApplicationContext(), title,
                message, pi);
        mNotificationManager.notify(DASHBOARD_NOTIFICATION_ID, notification);
    }
}
