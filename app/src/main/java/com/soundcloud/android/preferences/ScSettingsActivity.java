package com.soundcloud.android.preferences;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventBus;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public abstract class ScSettingsActivity extends PreferenceActivity {

    private static final String BUNDLE_CONFIGURATION_CHANGE = "BUNDLE_CONFIGURATION_CHANGE";

    private boolean mOnCreateCalled;
    private boolean mIsConfigurationChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(this.getClass()));

        mOnCreateCalled = true;
        if (savedInstanceState != null) {
            mIsConfigurationChange = savedInstanceState.getBoolean(BUNDLE_CONFIGURATION_CHANGE, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(this.getClass()));

        mOnCreateCalled = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnResume(this.getClass()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_CONFIGURATION_CHANGE, getChangingConfigurations() != 0);
    }

    protected boolean isReallyResuming() {
        return !mOnCreateCalled;
    }

    protected boolean isConfigurationChange() {
        return mIsConfigurationChange;
    }

    protected boolean shouldTrackScreen() {
        return !isConfigurationChange() || isReallyResuming();
    }

}
