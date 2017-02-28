package com.soundcloud.android.analytics.performance;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.onboarding.OnboardActivity;

import android.app.Activity;

enum UiLatencyMetric {
    MAIN_AUTHENTICATED, UNAUTHENTICATED, NONE;

    static UiLatencyMetric fromActivity(Activity activity) {
        final String fullActivityName = activity.getComponentName().getClassName();
        if (MainActivity.class.getName().equals(fullActivityName)) {
            return MAIN_AUTHENTICATED;
        } else if (OnboardActivity.class.getName().equals(fullActivityName)) {
            return UNAUTHENTICATED;
        } else {
            return NONE;
        }
    }
}
