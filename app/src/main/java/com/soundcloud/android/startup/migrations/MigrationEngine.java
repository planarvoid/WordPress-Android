package com.soundcloud.android.startup.migrations;

import static com.google.common.collect.Lists.newArrayList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.utils.AndroidUtils;

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

    private final SharedPreferences mSharedPreferences;
    private final int mCurrentVersion;
    private final List<Migration> mMigrations;

    public MigrationEngine(Context context) {
        this(AndroidUtils.getAppVersionCode(context,0) ,PreferenceManager.getDefaultSharedPreferences(context),
                new SettingsMigration(context));
    }

    @VisibleForTesting
    protected MigrationEngine(int currentVersion, SharedPreferences sharedPreferences,
                              Migration... migrationsToApply) {
        mSharedPreferences = sharedPreferences;
        mCurrentVersion = currentVersion;
        mMigrations = newArrayList(migrationsToApply);
    }

    public void migrate() {

        int previousVersionCode = mSharedPreferences.getInt(VERSION_KEY, DEFAULT_APP_VERSION_CODE);

        if (previousVersionCode != DEFAULT_APP_VERSION_CODE && previousVersionCode < mCurrentVersion) {
            List<Migration> applicableMigrations = newArrayList(Collections2.filter(mMigrations,
                    new ApplicableMigrationsPredicate(previousVersionCode, mCurrentVersion)));
            Collections.sort(applicableMigrations, Migration.APPLICABLE_VERSION_COMPARATOR);

            for(Migration migration : applicableMigrations){
                migration.applyMigration();
            }
        }

        updateVersionKey();
    }

    private void updateVersionKey() {
       Editor editor = mSharedPreferences.edit();
       editor.putInt(VERSION_KEY, mCurrentVersion);
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
