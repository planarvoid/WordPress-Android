package com.soundcloud.android.startup.migrations;

import com.soundcloud.android.settings.SettingKey;

import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

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
        editor.putBoolean(SettingKey.ANALYTICS_ENABLED, sharedPreferences.getBoolean(SettingKey.CRASH_REPORTING_ENABLED, true));
        editor.putBoolean(SettingKey.CRASH_REPORTING_ENABLED, sharedPreferences.getBoolean(CRASHLOGS_OLD_KEY, true));
        editor.apply();
    }

    @Override
    public int getApplicableAppVersionCode() {
        return 68;
    }
}
