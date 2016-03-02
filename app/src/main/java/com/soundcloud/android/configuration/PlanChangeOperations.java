package com.soundcloud.android.configuration;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.policies.PolicyOperations;
import rx.Observable;
import rx.functions.Action0;

import javax.inject.Inject;

public class PlanChangeOperations {

    private final ConfigurationOperations configurationOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PolicyOperations policyOperations;
    private final PlaySessionController playSessionController;

    private final Action0 clearPendingPlanChangeFlags = new Action0() {
        @Override
        public void call() {
            configurationOperations.clearPendingPlanChanges();
        }
    };

    private final Action0 resetPlaySession = new Action0() {
        @Override
        public void call() {
            playSessionController.resetPlaySession();
        }
    };

    @Inject
    PlanChangeOperations(ConfigurationOperations configurationOperations,
                         PolicyOperations policyOperations,
                         PlaySessionController playSessionController,
                         OfflineContentOperations offlineContentOperations) {
        this.configurationOperations = configurationOperations;
        this.policyOperations = policyOperations;
        this.playSessionController = playSessionController;
        this.offlineContentOperations = offlineContentOperations;
    }

    public Observable<Object> awaitAccountDowngrade() {
        return configurationOperations.awaitConfigurationFromPendingPlanChange()
                .flatMap(continueWith(offlineContentOperations.resetOfflineFeature()))
                .compose(new PlanChangedSteps());
    }

    public Observable<Object> awaitAccountUpgrade() {
        Observable<Configuration> updatedConfiguration;
        if (configurationOperations.isPendingHighTierUpgrade()) {
            // plan change occurred in background; await that configuration
            updatedConfiguration = configurationOperations.awaitConfigurationFromPendingPlanChange();
        } else {
            //TODO: handle mid-tier; right now, this can only be triggered by purchasing a high-tier
            // sub in the app
            updatedConfiguration = configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER);
        }
        return updatedConfiguration.compose(new PlanChangedSteps());
    }

    private final class PlanChangedSteps implements Observable.Transformer<Object, Object> {

        @Override
        public Observable<Object> call(Observable<Object> source) {
            return source.flatMap(continueWith(policyOperations.refreshedTrackPolicies()))
                    .doOnSubscribe(resetPlaySession)
                    .finallyDo(clearPendingPlanChangeFlags)
                    .cast(Object.class);
        }
    }
}
