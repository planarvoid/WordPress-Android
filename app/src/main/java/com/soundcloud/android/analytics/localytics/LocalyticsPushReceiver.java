package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.SoundCloudApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class LocalyticsPushReceiver extends BroadcastReceiver {

    @Inject
    LocalyticsAmpSession localyticsAmpSession;

    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);

        if(intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
            localyticsAmpSession.handleRegistration(intent);
        } else if(intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
            localyticsAmpSession.handleNotificationReceived(intent);
        }

    }
}
