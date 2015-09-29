package com.soundcloud.android.gcm;

import static com.appboy.push.AppboyNotificationUtils.APPBOY_NOTIFICATION_OPENED_SUFFIX;
import static com.appboy.push.AppboyNotificationUtils.APPBOY_NOTIFICATION_RECEIVED_SUFFIX;

import com.appboy.AppboyGcmReceiver;
import com.appboy.Constants;
import com.appboy.push.AppboyNotificationUtils;
import com.soundcloud.android.main.MainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class AppboyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "AppboyBroadcastReceiver";
    private static final String SOURCE_KEY = "source";
    private static final String APPLICATION_ID = "com.soundcloud.android";
    private static final String NOTIFICATION_OPENED = APPLICATION_ID + APPBOY_NOTIFICATION_OPENED_SUFFIX;
    private static final String NOTIFICATION_RECEIVED = APPLICATION_ID + APPBOY_NOTIFICATION_RECEIVED_SUFFIX;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (NOTIFICATION_RECEIVED.equals(action)) {
            handleNotificationReceived(intent);
        } else if (NOTIFICATION_OPENED.equals(action)) {
            handleNotificationOpened(context, intent);
        }
    }

    private void handleNotificationOpened(Context context, Intent intent) {
        Bundle extras = getPushExtrasBundle(intent);

        if (intent.getStringExtra(Constants.APPBOY_PUSH_DEEP_LINK_KEY) != null) {
            startDeepLinkActivities(context, intent, extras);
        } else {
            context.startActivity(getStartActivityIntent(context, extras));
        }
    }

    private void startDeepLinkActivities(Context context, Intent intent, Bundle extras) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(getStartActivityIntent(context, extras));
        stackBuilder.addNextIntent(getUriIntent(intent, extras));
        stackBuilder.startActivities(extras);
    }

    private Intent getUriIntent(Intent intent, Bundle extras) {
        Uri uri = Uri.parse(intent.getStringExtra(Constants.APPBOY_PUSH_DEEP_LINK_KEY));
        return new Intent(Intent.ACTION_VIEW, uri).putExtras(extras);
    }

    private void handleNotificationReceived(Intent intent) {
        Log.d(TAG, "Received push notification.");
        if (AppboyNotificationUtils.isUninstallTrackingPush(intent.getExtras())) {
            Log.d(TAG, "Got uninstall tracking push");
        }
    }

    private Intent getStartActivityIntent(Context context, Bundle extras) {
        Intent startActivityIntent = new Intent(context, MainActivity.class);
        startActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (extras != null) {
            startActivityIntent.putExtras(extras);
        }
        return startActivityIntent;
    }

    private Bundle getPushExtrasBundle(Intent intent) {
        Bundle extras = intent.getBundleExtra(Constants.APPBOY_PUSH_EXTRAS_KEY);
        if (extras == null) {
            extras = new Bundle();
        }
        extras.putString(AppboyGcmReceiver.CAMPAIGN_ID_KEY, intent.getStringExtra(AppboyGcmReceiver.CAMPAIGN_ID_KEY));
        extras.putString(SOURCE_KEY, Constants.APPBOY);
        return extras;
    }
}
