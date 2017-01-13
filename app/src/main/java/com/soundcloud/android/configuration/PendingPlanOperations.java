package com.soundcloud.android.configuration;

import javax.inject.Inject;

public class PendingPlanOperations {

    private final ConfigurationSettingsStorage configurationSettingsStorage;

    @Inject
    PendingPlanOperations(ConfigurationSettingsStorage configurationSettingsStorage) {
        this.configurationSettingsStorage = configurationSettingsStorage;
    }

    public void setPendingUpgrade(Plan plan) {
        configurationSettingsStorage.storePendingPlanUpgrade(plan);
    }

    void setPendingDowngrade(Plan plan) {
        configurationSettingsStorage.storePendingPlanDowngrade(plan);
    }

    boolean isPendingUpgrade() {
        final Plan pendingPlan = configurationSettingsStorage.getPendingPlanUpgrade();
        return pendingPlan == Plan.MID_TIER || pendingPlan == Plan.HIGH_TIER;
    }

    boolean isPendingDowngrade() {
        final Plan pendingPlan = configurationSettingsStorage.getPendingPlanDowngrade();
        return pendingPlan == Plan.FREE_TIER || pendingPlan == Plan.MID_TIER;
    }

    public Plan getPendingUpgrade() {
        return configurationSettingsStorage.getPendingPlanUpgrade();
    }

    public Plan getPendingDowngrade() {
        return configurationSettingsStorage.getPendingPlanDowngrade();
    }

    void clearPendingPlanChanges() {
        configurationSettingsStorage.clearPendingPlanChanges();
    }

    boolean hasPendingPlanChange() {
        final Plan pendingPlanDowngrade = configurationSettingsStorage.getPendingPlanDowngrade();
        final Plan pendingPlanUpgrade = configurationSettingsStorage.getPendingPlanUpgrade();
        return pendingPlanDowngrade != Plan.UNDEFINED || pendingPlanUpgrade != Plan.UNDEFINED;
    }

}
