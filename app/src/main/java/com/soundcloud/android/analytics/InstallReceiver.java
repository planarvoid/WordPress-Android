package com.soundcloud.android.analytics;

import com.adjust.sdk.ReferrerReceiver;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.localytics.LocalyticsReferralReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InstallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);

        new LocalyticsReferralReceiver().onReceive(context, intent);
        new ReferrerReceiver().onReceive(context, intent);
    }
}
