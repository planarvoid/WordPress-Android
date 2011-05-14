
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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

public class SyncAdapterService extends Service {
    private static final String TAG = "ScSyncAdapterService";
    private static ScSyncAdapter sSyncAdapter = null;
    private static ContentResolver mContentResolver = null;

    public static int DASHBOARD_NOTIFICATION_ID = R.layout.player_touch_bar;

    public SyncAdapterService() {
        super();
    }

    private static class ScSyncAdapter extends AbstractThreadedSyncAdapter {
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
            // it is easier to just enablling remote pulling only for now

            if (Build.VERSION.SDK_INT >= 8) {
                try {
                    SyncAdapterService.performSync(mApp, mContext, account, extras, authority,
                            provider, syncResult);
                } catch (OperationCanceledException e) {
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        ret = getSyncAdapter().getSyncAdapterBinder();
        return ret;
    }

    private ScSyncAdapter getSyncAdapter() {
        if (sSyncAdapter == null)
            sSyncAdapter = new ScSyncAdapter((SoundCloudApplication)this.getApplication(), this);
        return sSyncAdapter;
    }

    private static void performSync(final SoundCloudApplication app, Context context, Account account, Bundle extras,
            String authority, ContentProviderClient provider, SyncResult syncResult)
            throws OperationCanceledException {
        mContentResolver = context.getContentResolver();

        Log.i(TAG, "performSync: " + account.toString());

        app.useAccount(account);

        int incomingUnseen = app.getAccountDataInt(User.DataKeys.CURRENT_INCOMING_UNSEEN);
        int exclusiveUnseen = app.getAccountDataInt(User.DataKeys.CURRENT_EXCLUSIVE_UNSEEN);

        int newIncoming = 0;
        int newExclusive = 0;

        final long user_id = app.getAccountDataLong( User.DataKeys.USER_ID);
        try {
            newIncoming = SoundCloudDB.updateActivities(app, mContentResolver, user_id, false);
            newExclusive = SoundCloudDB.updateActivities(app, mContentResolver, user_id, true);

        } catch (JsonParseException e) {
            syncResult.stats.numParseExceptions++;
            e.printStackTrace();
        } catch (JsonMappingException e) {
            syncResult.stats.numParseExceptions++;
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
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

                    CharSequence title = "";
                    CharSequence message = "";
                    CharSequence ticker = "";
                    boolean gotoExclusive = false;

                    int exclusiveUnseen = app
                    .getAccountDataInt(User.DataKeys.CURRENT_EXCLUSIVE_UNSEEN);

                    int incomingUnseen = app
                    .getAccountDataInt(User.DataKeys.CURRENT_INCOMING_UNSEEN);

                    if (exclusiveUnseen + incomingUnseen == 0){
                        return;
                    } else if (incomingUnseen == 1){
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

                    if (message != "")  createDashboardNotification(app,ticker,title,message,gotoExclusive);

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

    private static void createDashboardNotification(SoundCloudApplication app, CharSequence ticker, CharSequence title, CharSequence message, boolean exclusive ){
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager)app
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

    //{"collection":[{"type":"track","created_at":"2011/05/02 16:33:18 +0000","origin":{"id":14559625,"created_at":"2011/05/02 16:33:18 +0000","user_id":2490,"duration":4416,"commentable":true,"state":"finished","sharing":"public","tag_list":"","permalink":"may_02_2011-001-edit","description":"","streamable":true,"downloadable":true,"genre":"","release":"","purchase_url":null,"label_id":null,"label_name":"","isrc":"","video_url":null,"track_type":"","key_signature":"","bpm":null,"title":"Quick Monday is quick","release_year":null,"release_month":null,"release_day":null,"original_format":"wav","license":"cc-by-nc-sa","uri":"https://api.soundcloud.com/tracks/14559625","permalink_url":"http://soundcloud.com/david/may_02_2011-001-edit","artwork_url":null,"waveform_url":"http://w1.sndcdn.com/sWJO1byxkgl5_m.png","user":{"id":2490,"permalink":"david","username":"David No\u00ebl","uri":"https://api.soundcloud.com/users/2490","permalink_url":"http://soundcloud.com/david","avatar_url":"http://i1.sndcdn.com/avatars-000003312251-vi5p6e-large.jpg?af2741b"},"stream_url":"https://api.soundcloud.com/tracks/14559625/stream","download_url":"https://api.soundcloud.com/tracks/14559625/download","user_playback_count":1,"user_favorite":false,"playback_count":32,"download_count":0,"favoritings_count":2,"comment_count":1,"created_with":{"id":3884,"name":"iRig Recorder","uri":"https://api.soundcloud.com/apps/3884","permalink_url":"http://soundcloud.com/apps/irig-recorder"},"attachments_uri":"https://api.soundcloud.com/tracks/14559625/attachments","sharing_note":{"text":"Mondays with @thisisparker are always a little bit faster","created_at":"2011/05/02 16:38:00 +0000"}},"tags":"affiliated"},{"type":"track","created_at":"2011/05/02 15:02:22 +0000","origin":{"id":14555387,"created_at":"2011/05/02 15:02:22 +0000","user_id":4606,"duration":366113,"commentable":true,"state":"finished","sharing":"public","tag_list":"nina simone feeling good hrdvsion remix sun closed eyes sleep forever","permalink":"nina-simone-feeling-good","description":"","streamable":true,"downloadable":true,"genre":"","release":"","purchase_url":null,"label_id":null,"label_name":"","isrc":"","video_url":null,"track_type":"remix","key_signature":"","bpm":null,"title":"Nina Simone - Feeling Good (Hrdvsion Remix)","release_year":null,"release_month":null,"release_day":null,"original_format":"mp3","license":"all-rights-reserved","uri":"https://api.soundcloud.com/tracks/14555387","permalink_url":"http://soundcloud.com/hrdvsion/nina-simone-feeling-good","artwork_url":"http://i1.sndcdn.com/artworks-000006889448-5kcm2w-large.jpg?af2741b","waveform_url":"http://w1.sndcdn.com/SzxkcLi84GTl_m.png","user":{"id":4606,"permalink":"hrdvsion","username":"Hrdvsion","uri":"https://api.soundcloud.com/users/4606","permalink_url":"http://soundcloud.com/hrdvsion","avatar_url":"http://i1.sndcdn.com/avatars-000000981338-vjhn9o-large.jpg?af2741b"},"stream_url":"https://api.soundcloud.com/tracks/14555387/stream","download_url":"https://api.soundcloud.com/tracks/14555387/download","user_playback_count":1,"user_favorite":false,"playback_count":186,"download_count":64,"favoritings_count":16,"comment_count":15,"attachments_uri":"https://api.soundcloud.com/tracks/14555387/attachments","sharing_note":{"text":"nina simone... sun, eyes closed in the brightness, sleep forever.","created_at":"2011/05/02 15:02:22 +0000"}},"tags":"affiliated"}],"next_href":"https://api.soundcloud.com/me/activities/tracks?cursor=c5751842-74a0-11e0-96d2-d41dad77532f\u0026limit=2"}

}
