package com.soundcloud.android.migrations;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.preferences.SettingsActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class SettingsMigration implements Migration {

    @VisibleForTesting
    protected static final String CRASHLOGS_OLD_KEY = "crashlogs";

    private final SharedPreferences mSharedPreferences;

    public SettingsMigration(Context context){
        this(PreferenceManager.getDefaultSharedPreferences(context));
    }

    @VisibleForTesting
    protected SettingsMigration(SharedPreferences defaultSharedPreferences) {
        mSharedPreferences = defaultSharedPreferences;

    }

    @Override
    public void applyMigration() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(SettingsActivity.ANALYTICS_ENABLED, mSharedPreferences.getBoolean(SettingsActivity.CRASH_REPORTING_ENABLED, true));
        editor.putBoolean(SettingsActivity.CRASH_REPORTING_ENABLED, mSharedPreferences.getBoolean(CRASHLOGS_OLD_KEY, true));
        editor.commit();
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 68;
    }
}
