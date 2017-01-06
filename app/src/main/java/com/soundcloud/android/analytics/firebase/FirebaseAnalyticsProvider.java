package com.soundcloud.android.analytics.firebase;


import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.annotations.VisibleForTesting;

import android.os.Bundle;
import android.util.Log;

import javax.inject.Inject;

public class FirebaseAnalyticsProvider extends DefaultAnalyticsProvider {
    private static final String TAG = FirebaseAnalyticsProvider.class.getSimpleName();

    @VisibleForTesting
    static final String DATA_IS_USER_LOGGED_IN = "logged_in_user";
    @VisibleForTesting
    static final String DATA_TIME_MILLIS = "time_in_millis";
    @VisibleForTesting
    static final String DATA_APP_VERSION = "app_version_name";
    @VisibleForTesting
    static final String DATA_DEVICE_NAME = "device_type_name";
    @VisibleForTesting
    static final String DATA_ANDROID_VERSION = "android_version";

    private final FirebaseAnalyticsWrapper firebaseAnalytics;

    @Inject
    FirebaseAnalyticsProvider(FirebaseAnalyticsWrapper firebaseAnalytics) {
        this.firebaseAnalytics = firebaseAnalytics;
    }

    @Override
    public void handlePerformanceEvent(PerformanceEvent event) {
        Log.d(TAG, String.format("Logging performance event: %s", event.name()));
        firebaseAnalytics.logEvent(event.name(), buildFirebaseEventData(event));
    }

    @VisibleForTesting
    Bundle buildFirebaseEventData(PerformanceEvent event) {
        final Bundle data = new Bundle();
        data.putBoolean(DATA_IS_USER_LOGGED_IN, event.userLoggedIn());
        data.putLong(DATA_TIME_MILLIS, event.timeMillis());
        data.putString(DATA_APP_VERSION, event.appVersionName());
        data.putString(DATA_DEVICE_NAME, event.deviceName());
        data.putString(DATA_ANDROID_VERSION, event.androidVersion());
        return data;
    }
}
