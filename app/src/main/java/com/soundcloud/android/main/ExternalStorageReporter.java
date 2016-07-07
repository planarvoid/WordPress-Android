package com.soundcloud.android.main;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ExternalDirectoryReportEvent;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

// this is to check if we can safely migrate our external directory handling to not require runtime permission
public class ExternalStorageReporter extends DefaultActivityLightCycle<AppCompatActivity> {

    private static final String HAS_REPORTED_KEY = "hasReportedExternalStorageConfig";
    private final SharedPreferences preferences;
    private final EventBus eventBus;

    @Inject
    public ExternalStorageReporter(SharedPreferences preferences, EventBus eventBus) {
        this.preferences = preferences;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        if (bundle == null && !preferences.getBoolean(HAS_REPORTED_KEY, false)) {
            preferences.edit().putBoolean(HAS_REPORTED_KEY, true).apply();
            eventBus.publish(EventQueue.TRACKING, new ExternalDirectoryReportEvent(activity));
        }
    }
}
