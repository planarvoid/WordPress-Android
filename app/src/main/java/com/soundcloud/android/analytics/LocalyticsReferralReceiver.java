package com.soundcloud.android.analytics;

import com.localytics.android.ReferralReceiver;

import android.content.Context;
import android.content.Intent;

public class LocalyticsReferralReceiver extends ReferralReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AnalyticsProperties analyticsProperties = new AnalyticsProperties(context.getResources());
        appKey = analyticsProperties.getLocalyticsAppKey();
        super.onReceive(context, intent);
    }
}
