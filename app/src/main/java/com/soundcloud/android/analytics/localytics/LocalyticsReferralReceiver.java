package com.soundcloud.android.analytics.localytics;

import com.localytics.android.ReferralReceiver;
import com.soundcloud.android.analytics.AnalyticsProperties;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LocalyticsReferralReceiver extends ReferralReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        AnalyticsProperties analyticsProperties = new AnalyticsProperties(context.getResources());
        appKey = analyticsProperties.getLocalyticsAppKey();
        super.onReceive(context, intent);
    }
}
