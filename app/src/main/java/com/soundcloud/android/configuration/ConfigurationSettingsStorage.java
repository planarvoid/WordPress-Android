package com.soundcloud.android.configuration;

import static com.soundcloud.android.storage.StorageModule.CONFIGURATION_SETTINGS;

import com.soundcloud.android.Consts;
import com.soundcloud.java.optional.Optional;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

class ConfigurationSettingsStorage {
    private static final String LAST_CONFIG_UPDATE_TIME = "last_config_check_time";
    private static final String KEY_PLAN_UPGRADE = "pending_plan_upgrade";
    private static final String KEY_PLAN_DOWNGRADE = "pending_plan_downgrade";

    private final SharedPreferences sharedPreferences;

    @Inject
    ConfigurationSettingsStorage(@Named(CONFIGURATION_SETTINGS) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
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

    Optional<Plan> getPendingPlanUpgrade() {
        return optionalPlan(sharedPreferences.getString(KEY_PLAN_UPGRADE, null));
    }

    Optional<Plan> getPendingPlanDowngrade() {
        return optionalPlan(sharedPreferences.getString(KEY_PLAN_DOWNGRADE, null));
    }

    private static Optional<Plan> optionalPlan(String planId) {
        if (planId == null) {
            return Optional.absent();
        }
        return Optional.of(Plan.fromId(planId));
    }
}
