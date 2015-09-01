package com.soundcloud.android.gcm;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.SoundCloudApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class GcmMessageReceiver extends BroadcastReceiver {

    private static final String REGISTRATION_ACTION = "com.google.android.c2dm.intent.REGISTRATION";
    private static final String RECEIVE_MESSSAGE_ACTION = "com.google.android.c2dm.intent.RECEIVE";

    @Inject
    LocalyticsAmpSession localyticsAmpSession;

    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);

        if(REGISTRATION_ACTION.equals(intent.getAction())) {
            localyticsAmpSession.handleRegistration(intent);

        } else if(RECEIVE_MESSSAGE_ACTION.equals(intent.getAction())) {
            localyticsAmpSession.handleNotificationReceived(intent);
        }

    }
}
