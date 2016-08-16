package com.soundcloud.android.main;

import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.TheTracker;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class ForegroundController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final TheTracker theTracker;
    private static final String EXTRA_HAS_TRACKED = "hasTrackedForeground";

    @Inject
    public ForegroundController(TheTracker theTracker) {
        this.theTracker = theTracker;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        trackForegroundEvent(activity.getIntent());
    }

    @Override
    public void onNewIntent(AppCompatActivity activity, Intent intent) {
        trackForegroundEvent(intent);
    }

    private void trackForegroundEvent(Intent intent) {
        if (!intent.getBooleanExtra(EXTRA_HAS_TRACKED, false) && Referrer.hasReferrer(intent) && Screen.hasScreen(intent)) {
            theTracker.trackForegroundEvent(ForegroundEvent.open(Screen.fromIntent(intent), Referrer.fromIntent(intent)));
            intent.putExtra(EXTRA_HAS_TRACKED, true);
        }
    }
}
