package com.soundcloud.android.configuration;

import static com.soundcloud.android.configuration.ConfigurationOperations.TAG;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class PlanChangeDetector {

    private final EventBus eventBus;
    private final FeatureOperations featureOperations;
    private final PendingPlanOperations pendingPlanOperations;

    @Inject
    PlanChangeDetector(EventBus eventBus,
                       FeatureOperations featureOperations,
                       PendingPlanOperations pendingPlanOperations) {
        this.eventBus = eventBus;
        this.featureOperations = featureOperations;
        this.pendingPlanOperations = pendingPlanOperations;
    }

    public void handleRemotePlan(Plan remotePlan) {
        if (!pendingPlanOperations.hasPendingPlanChange()) {
            final Plan currentPlan = featureOperations.getCurrentPlan();
            if (remotePlan.isUpgradeFrom(currentPlan)) {
                Log.d(TAG, "Plan upgrade detected from " + currentPlan + " to " + remotePlan);
                pendingPlanOperations.setPendingUpgrade(remotePlan);
                eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forUpgrade(currentPlan, remotePlan));
            } else if (remotePlan.isDowngradeFrom(currentPlan)) {
                Log.d(TAG, "Plan downgrade detected from " + currentPlan + " to " + remotePlan);
                pendingPlanOperations.setPendingDowngrade(remotePlan);
                eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forDowngrade(currentPlan, remotePlan));
            }
        }
    }

}
