package com.soundcloud.android.configuration;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.ClearTrackDownloadsCommand;
import com.soundcloud.android.policies.PolicyOperations;
import rx.Observable;
import rx.functions.Action0;

import javax.inject.Inject;
import java.util.List;

public class PlanChangeOperations {

    private final ConfigurationOperations configurationOperations;
    private final PolicyOperations policyOperations;
    private final ClearTrackDownloadsCommand clearTrackDownloadsCommand;
    private final Action0 clearPendingPlanChangeFlags = new Action0() {
        @Override
        public void call() {
            configurationOperations.clearPendingPlanChanges();
        }
    };

    @Inject
    PlanChangeOperations(ConfigurationOperations configurationOperations,
                         PolicyOperations policyOperations,
                         ClearTrackDownloadsCommand clearTrackDownloadsCommand) {
        this.configurationOperations = configurationOperations;
        this.policyOperations = policyOperations;
        this.clearTrackDownloadsCommand = clearTrackDownloadsCommand;
    }

    public Observable<Object> awaitAccountDowngrade() {
        return policyOperations.refreshedTrackPolicies()
                .flatMap(continueWith(clearTrackDownloadsCommand.toObservable(null)))
                .doOnCompleted(clearPendingPlanChangeFlags)
                .cast(Object.class);
    }

    public Observable<List<Urn>> awaitAccountUpgrade() {
        Observable<List<Urn>> updatedAccountData;
        if (configurationOperations.isPendingHighTierUpgrade()) {
            // we already know the plans have changed; no need to await a plan change
            updatedAccountData = policyOperations.refreshedTrackPolicies();
        } else {
            updatedAccountData = configurationOperations
                    .awaitConfigurationWithPlan(Plan.HIGH_TIER)
                    .flatMap(continueWith(policyOperations.refreshedTrackPolicies()));
        }
        return updatedAccountData.doOnCompleted(clearPendingPlanChangeFlags);
    }
}
