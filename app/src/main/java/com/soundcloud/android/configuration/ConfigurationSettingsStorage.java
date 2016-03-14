package com.soundcloud.android.configuration;

import static com.soundcloud.android.storage.StorageModule.CONFIGURATION_SETTINGS;

import com.soundcloud.android.Consts;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
class ConfigurationSettingsStorage {
    private static final String LAST_CONFIG_UPDATE_TIME = "last_config_check_time";
    private static final String KEY_PLAN_UPGRADE = "pending_plan_upgrade";
    private static final String KEY_PLAN_DOWNGRADE = "pending_plan_downgrade";
    private static final String KEY_FORCE_UPDATE_VERSION = "force_update_version";

    private final SharedPreferences sharedPreferences;

    @Inject
    ConfigurationSettingsStorage(@Named(CONFIGURATION_SETTINGS) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    void clear() {
        sharedPreferences.edit().clear().apply();
    }

    void setLastConfigurationUpdateTime(long timestamp) {
        sharedPreferences.edit().putLong(LAST_CONFIG_UPDATE_TIME, timestamp).apply();
    }

    long getLastConfigurationCheckTime() {
        return sharedPreferences.getLong(LAST_CONFIG_UPDATE_TIME, Consts.NOT_SET);
    }

    void storePendingPlanUpgrade(Plan newPlan) {
        clearPendingPlanChanges();
        sharedPreferences.edit().putString(KEY_PLAN_UPGRADE, newPlan.planId).apply();
    }

    void storePendingPlanDowngrade(Plan newPlan) {
        clearPendingPlanChanges();
        sharedPreferences.edit().putString(KEY_PLAN_DOWNGRADE, newPlan.planId).apply();
    }

    void clearPendingPlanChanges() {
        sharedPreferences.edit()
                .remove(KEY_PLAN_DOWNGRADE)
                .remove(KEY_PLAN_UPGRADE)
                .apply();
    }

    Plan getPendingPlanUpgrade() {
        return Plan.fromId(sharedPreferences.getString(KEY_PLAN_UPGRADE, null));
    }

    Plan getPendingPlanDowngrade() {
        return Plan.fromId(sharedPreferences.getString(KEY_PLAN_DOWNGRADE, null));
    }

    void storeForceUpdateVersion(int appVersionCode) {
        sharedPreferences.edit().putInt(KEY_FORCE_UPDATE_VERSION, appVersionCode).apply();
    }

    int getForceUpdateVersion() {
        return sharedPreferences.getInt(KEY_FORCE_UPDATE_VERSION, 0);
    }

    void clearForceUpdateVersion() {
        sharedPreferences.edit().remove(KEY_FORCE_UPDATE_VERSION).apply();
    }
}
