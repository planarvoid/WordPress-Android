package com.soundcloud.android.startup.migrations;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.settings.GeneralSettings;

import android.content.SharedPreferences;

import javax.inject.Inject;

class SettingsMigration implements Migration {

    @VisibleForTesting
    protected static final String CRASHLOGS_OLD_KEY = "crashlogs";

    private final SharedPreferences sharedPreferences;

    @Inject
    SettingsMigration(SharedPreferences sharedPreferences){
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public void applyMigration() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(GeneralSettings.ANALYTICS_ENABLED, sharedPreferences.getBoolean(GeneralSettings.CRASH_REPORTING_ENABLED, true));
        editor.putBoolean(GeneralSettings.CRASH_REPORTING_ENABLED, sharedPreferences.getBoolean(CRASHLOGS_OLD_KEY, true));
        editor.apply();
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 68;
    }
}
