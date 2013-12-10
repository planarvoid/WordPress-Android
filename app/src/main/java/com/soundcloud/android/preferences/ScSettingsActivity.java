package com.soundcloud.android.preferences;

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
    protected void onPause() {
        super.onPause();
        mOnCreateCalled = false;
        mIsFirstRun = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected boolean isReallyResuming() {
        return !mOnCreateCalled;
    }

    protected boolean isConfigurationChange() {
        return !mIsFirstRun && mOnCreateCalled;
    }

}
