package com.soundcloud.android.main;

import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class ForegroundController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final EventBus eventBus;

    @Inject
    public ForegroundController(EventBus eventBus) {
        this.eventBus = eventBus;
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
        if (Referrer.hasReferrer(intent) && Screen.hasScreen(intent)) {
            ForegroundEvent event = ForegroundEvent.open(Screen.fromIntent(intent), Referrer.fromIntent(intent));
            eventBus.publish(EventQueue.TRACKING, event);
            Referrer.removeFromIntent(intent);
        }
    }

}
