package com.soundcloud.android.main;

import com.soundcloud.android.lightcycle.DefaultActivityLightCycle;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

class ScreenStateLightCycle extends DefaultActivityLightCycle {
    private static final String BUNDLE_CONFIGURATION_CHANGE = "BUNDLE_CONFIGURATION_CHANGE";

    private boolean onCreateCalled = false;
    private boolean isConfigurationChange = false;
    private boolean isForeground = false;

    @Inject
    public ScreenStateLightCycle() {
        // For dagger
    }

    boolean isForeground() {
        return isForeground;
    }

    boolean isReallyResuming() {
        return !onCreateCalled;
    }

    boolean isConfigurationChange() {
        return isConfigurationChange;
    }

    @Override
    public void onCreate(FragmentActivity activity, @Nullable Bundle savedInstanceState) {
        onCreateCalled = true;
        if (savedInstanceState != null) {
            isConfigurationChange = savedInstanceState.getBoolean(BUNDLE_CONFIGURATION_CHANGE, false);
        }
    }

    @Override
    public void onSaveInstanceState(FragmentActivity activity, Bundle outState) {
        // XXX : This is false in some situations where we seem to actually be changing configurations
        // (hit the power off button on a genymotion emulator while in landscape). This is not conclusive yet. Investigating further
        outState.putBoolean(BUNDLE_CONFIGURATION_CHANGE, activity.getChangingConfigurations() != 0);
    }

    @Override
    public void onResume(FragmentActivity activity) {
        isForeground = true;
    }

    @Override
    public void onPause(FragmentActivity activity) {
        onCreateCalled = false;
        isForeground = false;
    }
}
