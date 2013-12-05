package com.soundcloud.android.preferences;

import com.soundcloud.android.rx.Event;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public abstract class ScSettingsActivity extends PreferenceActivity {

    private boolean mOnCreateCalled;
    private boolean mIsFirstRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOnCreateCalled = true;
        mIsFirstRun = savedInstanceState == null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isConfigurationChange() || isReallyResuming()) {
            Event.SCREEN_ENTERED.publish(getTrackingName());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOnCreateCalled = false;
        mIsFirstRun = false;
    }

    protected abstract String getTrackingName();

    protected boolean isReallyResuming() {
        return !mOnCreateCalled;
    }

    protected boolean isConfigurationChange() {
        return !mIsFirstRun && mOnCreateCalled;
    }

}
