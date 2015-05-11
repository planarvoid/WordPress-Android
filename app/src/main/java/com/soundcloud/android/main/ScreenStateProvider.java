package com.soundcloud.android.main;

import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import javax.inject.Inject;

class ScreenStateProvider extends DefaultLightCycleActivity<ActionBarActivity> {
    private static final String BUNDLE_CONFIGURATION_CHANGE = "BUNDLE_CONFIGURATION_CHANGE";

    private boolean onCreateCalled;
    private boolean isConfigurationChange;
    private boolean isForeground;

    @Inject
    public ScreenStateProvider() {
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
    public void onCreate(ActionBarActivity activity, @Nullable Bundle savedInstanceState) {
        onCreateCalled = true;
        if (savedInstanceState != null) {
            isConfigurationChange = savedInstanceState.getBoolean(BUNDLE_CONFIGURATION_CHANGE, false);
        }
    }

    @Override
    public void onSaveInstanceState(ActionBarActivity activity, Bundle outState) {
        // XXX : This is false in some situations where we seem to actually be changing configurations
        // (hit the power off button on a genymotion emulator while in landscape). This is not conclusive yet. Investigating further
        outState.putBoolean(BUNDLE_CONFIGURATION_CHANGE, activity.getChangingConfigurations() != 0);
    }

    @Override
    public void onResume(ActionBarActivity activity) {
        isForeground = true;
    }

    @Override
    public void onPause(ActionBarActivity activity) {
        onCreateCalled = false;
        isForeground = false;
    }
}
