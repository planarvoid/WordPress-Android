package com.soundcloud.android.migrations;

import static android.content.SharedPreferences.Editor;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.utils.AndroidUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsMigration {
    @VisibleForTesting
    protected static final String VERSION_KEY = "changeLogVersionCode";
    @VisibleForTesting
    protected static final int APPLIED_VERSION = 68;
    @VisibleForTesting
    protected static final String CRASHLOGS_OLD_KEY = "crashlogs";
    public static final int FIRST_TIME_INSTALL_FLAG = -1;

    private final SharedPreferences mSharedPreferences;
    private final int mCurrentVersion;

    public SettingsMigration(Context context) {
        this(AndroidUtils.getAppVersionCode(context,0) ,PreferenceManager.getDefaultSharedPreferences(context));
    }

    public SettingsMigration(int currentVersion, SharedPreferences sharedPreferences) {
        mSharedPreferences = sharedPreferences;
        mCurrentVersion = currentVersion;
    }

    public void migrate() {

        int previousVersionCode = mSharedPreferences.getInt(VERSION_KEY, FIRST_TIME_INSTALL_FLAG);

        if (previousVersionCode != FIRST_TIME_INSTALL_FLAG && previousVersionCode < APPLIED_VERSION) {
            Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Settings.ANALYTICS_ENABLED, mSharedPreferences.getBoolean(Settings.CRASH_REPORTING_ENABLED, true));
            editor.putBoolean(Settings.CRASH_REPORTING_ENABLED, mSharedPreferences.getBoolean(CRASHLOGS_OLD_KEY, true));
            editor.commit();
        }

        updateVersionKey();
    }

    private void updateVersionKey() {
       Editor editor = mSharedPreferences.edit();
       editor.putInt(VERSION_KEY, mCurrentVersion);
       editor.commit();
    }

}
