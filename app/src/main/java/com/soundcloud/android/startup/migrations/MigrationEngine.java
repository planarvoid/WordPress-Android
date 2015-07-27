package com.soundcloud.android.startup.migrations;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Predicate;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class MigrationEngine {
    @VisibleForTesting
    protected static final String VERSION_KEY = "changeLogVersionCode";
    private static final int DEFAULT_APP_VERSION_CODE = -1;

    private final SharedPreferences sharedPreferences;
    private final int currentVersion;
    private final List<Migration> migrations;

    @Inject
    public MigrationEngine(DeviceHelper deviceHelper, SharedPreferences sharedPreferences,
                           SettingsMigration settingsMigration, DiskCacheMigration diskCacheMigration) {
        this(deviceHelper.getAppVersionCode(),
                sharedPreferences,
                settingsMigration, diskCacheMigration);
    }

    @VisibleForTesting
    MigrationEngine(int currentVersion, SharedPreferences sharedPreferences,
                              Migration... migrationsToApply) {
        this.sharedPreferences = sharedPreferences;
        this.currentVersion = currentVersion;
        migrations = newArrayList(migrationsToApply);
    }

    public void migrate() {

        int previousVersionCode = sharedPreferences.getInt(VERSION_KEY, DEFAULT_APP_VERSION_CODE);

        if (previousVersionCode != DEFAULT_APP_VERSION_CODE && previousVersionCode < currentVersion) {
            List<Migration> applicableMigrations = newArrayList(MoreCollections.filter(migrations,
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

    private static class ApplicableMigrationsPredicate implements Predicate<Migration> {

        private final int previousVersionCode;
        private final int currentVersion;

        public ApplicableMigrationsPredicate(int previousVersionCode, int currentVersion) {
            this.previousVersionCode = previousVersionCode;
            this.currentVersion = currentVersion;
        }

        @Override
        public boolean apply(Migration input) {
            return input.getApplicableAppVersionCode() > previousVersionCode && input.getApplicableAppVersionCode() <= currentVersion;
        }
    }

}
