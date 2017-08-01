package com.soundcloud.android.configuration;

import static com.soundcloud.android.utils.ErrorUtils.isNetworkError;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;

import javax.inject.Inject;

public class PlanChangeOperations {

    private final ConfigurationOperations configurationOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PendingPlanOperations pendingPlanOperations;
    private final PolicyOperations policyOperations;
    private final PlaySessionController playSessionController;
    private final EventBusV2 eventBus;

    @Inject
    PlanChangeOperations(ConfigurationOperations configurationOperations,
                         PendingPlanOperations pendingPlanOperations,
                         PolicyOperations policyOperations,
                         PlaySessionController playSessionController,
                         OfflineContentOperations offlineContentOperations,
                         EventBusV2 eventBus) {
        this.configurationOperations = configurationOperations;
        this.pendingPlanOperations = pendingPlanOperations;
        this.policyOperations = policyOperations;
        this.playSessionController = playSessionController;
        this.offlineContentOperations = offlineContentOperations;
        this.eventBus = eventBus;
    }

    public Observable<Object> awaitAccountDowngrade() {
        return RxJava.toV2Observable(configurationOperations.awaitConfigurationFromPendingDowngrade())
                     .flatMap(configuration -> configuration.getUserPlan().currentPlan == Plan.FREE_TIER
                                               ? offlineContentOperations.disableOfflineFeature().toObservable()
                                               : Observable.just(configuration))
                     .compose(new PlanChangedSteps());
    }

    public Observable<Object> awaitAccountUpgrade() {
        return RxJava.toV2Observable(configurationOperations.awaitConfigurationFromPendingUpgrade())
                     .compose(new PlanChangedSteps());
    }

    /*
     * Publishes policy change event, triggering the offline service if we downgraded to mid-tier.
     * That's a no-op if we downgraded to free, since that config change unsubscribes OfflineContentController.
     */
    private final class PlanChangedSteps implements ObservableTransformer<Object, Object> {
        @Override
        public Observable<Object> apply(Observable<Object> source) {
            return source.flatMap(o -> RxJava.toV2Observable(policyOperations.refreshedTrackPolicies()))
                         .doOnNext(urns -> eventBus.publish(EventQueue.POLICY_UPDATES, PolicyUpdateEvent.create(urns)))
                         .doOnSubscribe(ignore -> playSessionController.resetPlaySession())
                         .doOnComplete(pendingPlanOperations::clearPendingPlanChanges)
                         .doOnError(throwable -> {
                             // we retry network errors, so don't treat this as terminal
                             if (!isNetworkError(throwable)) {
                                 pendingPlanOperations.clearPendingPlanChanges();
                             }
                         })
                         .cast(Object.class);
        }
    }

}
