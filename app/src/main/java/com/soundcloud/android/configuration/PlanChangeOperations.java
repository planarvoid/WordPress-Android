package com.soundcloud.android.configuration;

import static com.soundcloud.android.utils.ErrorUtils.isNetworkError;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.functions.Action1;

import javax.inject.Inject;

public class PlanChangeOperations {

    private final ConfigurationOperations configurationOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PendingPlanOperations pendingPlanOperations;
    private final PolicyOperations policyOperations;
    private final PlaySessionController playSessionController;
    private final EventBus eventBus;

    private Action1<Throwable> clearPendingPlanChangeFlagsIfUnrecoverableError = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            // we retry network errors, so don't treat this as terminal
            if (!isNetworkError(throwable)) {
                pendingPlanOperations.clearPendingPlanChanges();
            }
        }
    };

    @Inject
    PlanChangeOperations(ConfigurationOperations configurationOperations,
                         PendingPlanOperations pendingPlanOperations,
                         PolicyOperations policyOperations,
                         PlaySessionController playSessionController,
                         OfflineContentOperations offlineContentOperations,
                         EventBus eventBus) {
        this.configurationOperations = configurationOperations;
        this.pendingPlanOperations = pendingPlanOperations;
        this.policyOperations = policyOperations;
        this.playSessionController = playSessionController;
        this.offlineContentOperations = offlineContentOperations;
        this.eventBus = eventBus;
    }

    public Observable<Object> awaitAccountDowngrade() {
        return configurationOperations.awaitConfigurationFromPendingDowngrade()
                                      .flatMap(configuration -> configuration.getUserPlan().currentPlan == Plan.FREE_TIER
                                             ? offlineContentOperations.disableOfflineFeature()
                                             : Observable.just(configuration))
                                      .compose(new PlanChangedSteps());
    }

    public Observable<Object> awaitAccountUpgrade() {
        return configurationOperations.awaitConfigurationFromPendingUpgrade()
                .compose(new PlanChangedSteps());
    }

    /*
     * Publishes policy change event, triggering the offline service if we downgraded to mid-tier.
     * That's a no-op if we downgraded to free, since that config change unsubscribes OfflineContentController.
     */
    private final class PlanChangedSteps implements Observable.Transformer<Object, Object> {
        @Override
        public Observable<Object> call(Observable<Object> source) {
            return source.flatMap(o -> policyOperations.refreshedTrackPolicies())
                         .doOnNext(urns -> eventBus.publish(EventQueue.POLICY_UPDATES, PolicyUpdateEvent.create(urns)))
                         .doOnSubscribe(playSessionController::resetPlaySession)
                         .doOnCompleted(pendingPlanOperations::clearPendingPlanChanges)
                         .doOnError(clearPendingPlanChangeFlagsIfUnrecoverableError)
                         .cast(Object.class);
        }
    }

}
