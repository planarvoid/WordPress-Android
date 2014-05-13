package com.soundcloud.android.preferences;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import javax.inject.Inject;

public abstract class ScSettingsActivity extends PreferenceActivity {

    private static final String BUNDLE_CONFIGURATION_CHANGE = "BUNDLE_CONFIGURATION_CHANGE";

    private boolean onCreateCalled;
    private boolean isConfigurationChange;

    @Inject
    EventBus eventBus;

    public ScSettingsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ScSettingsActivity(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(this.getClass()));

        onCreateCalled = true;
        if (savedInstanceState != null) {
            isConfigurationChange = savedInstanceState.getBoolean(BUNDLE_CONFIGURATION_CHANGE, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(this.getClass()));

        onCreateCalled = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(this.getClass()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_CONFIGURATION_CHANGE, getChangingConfigurations() != 0);
    }

    protected boolean isReallyResuming() {
        return !onCreateCalled;
    }

    protected boolean isConfigurationChange() {
        return isConfigurationChange;
    }

    protected boolean shouldTrackScreen() {
        return !isConfigurationChange() || isReallyResuming();
    }

}
