package com.soundcloud.android.configuration;

import static com.soundcloud.android.configuration.ConfigurationOperations.TAG;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class PlanChangeDetector {

    private final EventBus eventBus;
    private final FeatureOperations featureOperations;
    private final ConfigurationSettingsStorage configurationSettingsStorage;

    @Inject
    PlanChangeDetector(EventBus eventBus,
                       FeatureOperations featureOperations,
                       ConfigurationSettingsStorage configurationSettingsStorage) {
        this.eventBus = eventBus;
        this.featureOperations = featureOperations;
        this.configurationSettingsStorage = configurationSettingsStorage;
    }

    public void handleRemotePlan(Plan remotePlan) {
        if (!hasPendingPlanChanges()) {
            final Plan currentPlan = featureOperations.getCurrentPlan();
            if (remotePlan.isUpgradeFrom(currentPlan)) {
                Log.d(TAG, "Plan upgrade detected from " + currentPlan + " to " + remotePlan);
                configurationSettingsStorage.storePendingPlanUpgrade(remotePlan);
                eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forUpgrade(currentPlan, remotePlan));
            } else if (remotePlan.isDowngradeFrom(currentPlan)) {
                Log.d(TAG, "Plan downgrade detected from " + currentPlan + " to " + remotePlan);
                configurationSettingsStorage.storePendingPlanDowngrade(remotePlan);
                eventBus.publish(EventQueue.USER_PLAN_CHANGE,
                                 UserPlanChangedEvent.forDowngrade(currentPlan, remotePlan));
            }
        }
    }

    private boolean hasPendingPlanChanges() {
        final Plan pendingPlanDowngrade = configurationSettingsStorage.getPendingPlanDowngrade();
        final Plan pendingPlanUpgrade = configurationSettingsStorage.getPendingPlanUpgrade();
        return pendingPlanDowngrade != Plan.UNDEFINED || pendingPlanUpgrade != Plan.UNDEFINED;
    }
}
