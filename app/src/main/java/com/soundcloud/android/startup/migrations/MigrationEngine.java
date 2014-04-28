package com.soundcloud.android.startup.migrations;

import static com.google.common.collect.Lists.newArrayList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.List;

public class MigrationEngine {
    @VisibleForTesting
    protected static final String VERSION_KEY = "changeLogVersionCode";
    private static final int DEFAULT_APP_VERSION_CODE = -1;

    private final SharedPreferences sharedPreferences;
    private final int currentVersion;
    private final List<Migration> migrations;

    public MigrationEngine(Context context) {
        this(new DeviceHelper(context).getAppVersionCode(), PreferenceManager.getDefaultSharedPreferences(context),
                new SettingsMigration(context));
    }

    @VisibleForTesting
    protected MigrationEngine(int currentVersion, SharedPreferences sharedPreferences,
                              Migration... migrationsToApply) {
        this.sharedPreferences = sharedPreferences;
        this.currentVersion = currentVersion;
        migrations = newArrayList(migrationsToApply);
    }

    public void migrate() {

        int previousVersionCode = sharedPreferences.getInt(VERSION_KEY, DEFAULT_APP_VERSION_CODE);

        if (previousVersionCode != DEFAULT_APP_VERSION_CODE && previousVersionCode < currentVersion) {
            List<Migration> applicableMigrations = newArrayList(Collections2.filter(migrations,
                    new ApplicableMigrationsPredicate(previousVersionCode, currentVersion)));
            Collections.sort(applicableMigrations, Migration.APPLICABLE_VERSION_COMPARATOR);

            for(Migration migration : applicableMigrations){
                migration.applyMigration();
            }
        }

        updateVersionKey();
    }

    private void updateVersionKey() {
       Editor editor = sharedPreferences.edit();
       editor.putInt(VERSION_KEY, currentVersion);
       editor.apply();
    }

    private static class ApplicableMigrationsPredicate implements Predicate<Migration>{

        private final int mPreviousVersionCode;
        private final int mCurrentVersion;

        public ApplicableMigrationsPredicate(int previousVersionCode, int currentVersion) {
            mPreviousVersionCode = previousVersionCode;
            mCurrentVersion = currentVersion;
        }

        @Override
        public boolean apply(Migration input) {
            return input.getApplicableAppVersionCode() > mPreviousVersionCode && input.getApplicableAppVersionCode() <= mCurrentVersion;
        }
    }

}
