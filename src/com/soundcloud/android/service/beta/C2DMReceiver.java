package com.soundcloud.android.service.beta;

import static com.soundcloud.android.SoundCloudApplication.handleSilentException;
import static com.soundcloud.android.service.beta.BetaService.TAG;
import static com.soundcloud.android.service.beta.BetaService.setPendingBeta;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Settings;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class C2DMReceiver extends BroadcastReceiver {
    public static final String PREF_REG_ID = "c2dm.reg_id";
    public static final String SENDER = "jan@soundcloud.com";

    public static final String ACTION_REGISTER = "com.google.android.c2dm.intent.REGISTER";
    public static final String ACTION_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";
    public static final String ACTION_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";

    public static final String EXTRA_BETA_VERSION = "beta-version";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_REGISTRATION)) {
            onRegister(context, intent);
        } else if (intent.getAction().equals(ACTION_RECEIVE)) {
            onReceiveMessage(context, intent);
        }
    }

    private void onRegister(Context context, Intent intent) {
        String error = intent.getStringExtra("error");
        if (error != null) {
            Log.w(TAG, "error registering with C2DM: "+error);
        } else  if (intent.hasExtra("unregistered")) {
            Log.d(TAG, "application has unregistered");
            PreferenceManager.getDefaultSharedPreferences(context)
                             .edit()
                             .remove(PREF_REG_ID)
                             .commit();
        } else {
            String regId = intent.getStringExtra("registration_id");
            Log.d(TAG, "registrationId:"+regId);
            if (regId != null) {

                PreferenceManager.getDefaultSharedPreferences(context)
                                 .edit()
                                 .putString(PREF_REG_ID, regId)
                                 .commit();

                // cheap way to get registration back to us - use acra
                handleSilentException("registration_id=" + regId, null);
            } else {
                Log.w(TAG, "no registration id received");
            }
        }
    }


    private void onReceiveMessage(Context context, Intent intent) {
        Log.d(TAG, "onReceiveMessage("+intent+")");
        // TODO hold wakelock
        if (intent.hasExtra(EXTRA_BETA_VERSION)) {
            String beta = intent.getStringExtra(EXTRA_BETA_VERSION);
            String[] parts = beta.split(":",2);
            if (parts.length == 2) {
                try {
                    int versionCode    = Integer.parseInt(parts[0]);
                    String versionName = parts[1];
                    if (!Content.isUptodate(context, versionCode, versionName)) {
                        //notifyNewVersion(context, versionName + "  ("+versionCode+")");
                        setPendingBeta(context, versionName);
                        BetaService.scheduleNow(context, 2000l);
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "could not parse version information");
                }
            } else {
                Log.w(TAG, "could not parse "+EXTRA_BETA_VERSION);
            }
        }
    }

   /** @noinspection UnusedDeclaration*/
   private void notifyNewVersion(Context context, String version) {
        String title = context.getString(R.string.pref_beta_new_version_available);
        Intent intent = new Intent(context, Settings.class)
                 .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Notification n = new Notification(R.drawable.statusbar, title, System.currentTimeMillis());
        n.flags |= BetaService.defaultNotificationFlags();
        n.setLatestEventInfo(context, title, version, PendingIntent.getActivity(context, 0, intent ,0 ));
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(Consts.Notifications.BETA_NOTIFY_ID, n);
    }

    public static void register(Context context) {
        if (Build.VERSION.SDK_INT < 8) return;
        String regId = PreferenceManager.getDefaultSharedPreferences(context)
                                       .getString(PREF_REG_ID, null);

        if (regId == null) {
            Log.d(TAG, "registering for c2dm");

            Intent reg = new Intent(ACTION_REGISTER)
                        .putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0))
                        .putExtra("sender", C2DMReceiver.SENDER);

            context.startService(reg);
        } else {
            Log.d(TAG, "device is already registered with "+regId);
        }
    }
}
