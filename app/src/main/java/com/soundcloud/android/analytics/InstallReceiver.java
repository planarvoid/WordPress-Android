package com.soundcloud.android.analytics;

import com.adjust.sdk.ReferrerReceiver;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.localytics.LocalyticsReferralReceiver;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class InstallReceiver extends BroadcastReceiver {

    @Inject FeatureFlags featureFlags;

    @Override
    public void onReceive(Context context, Intent intent) {
        SoundCloudApplication.getObjectGraph().inject(this);

        new LocalyticsReferralReceiver().onReceive(context, intent);

        if (featureFlags.isEnabled(Feature.ADJUST_TRACKING)) {
            new ReferrerReceiver().onReceive(context, intent);
        }
    }
}
