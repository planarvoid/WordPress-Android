package com.soundcloud.android.analytics.firebase;

import com.google.firebase.analytics.FirebaseAnalytics;

import android.os.Bundle;
import android.support.annotation.NonNull;

import javax.inject.Inject;

public class FirebaseAnalyticsWrapper {

    private final FirebaseAnalytics firebaseAnalytics;

    @Inject
    FirebaseAnalyticsWrapper(FirebaseAnalytics firebaseAnalytics) {
        this.firebaseAnalytics = firebaseAnalytics;
    }

    public void logEvent(@NonNull String name, Bundle data) {
        firebaseAnalytics.logEvent(name, data);
    }
}
