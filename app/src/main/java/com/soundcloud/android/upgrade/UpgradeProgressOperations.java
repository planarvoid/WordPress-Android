package com.soundcloud.android.upgrade;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import rx.Observable;
import rx.functions.Action0;

import javax.inject.Inject;
import java.util.List;

class UpgradeProgressOperations {

    private final ConfigurationOperations configurationOperations;
    private final PolicyOperations policyOperations;
    private final Action0 clearPendingPlanChangeFlags = new Action0() {
        @Override
        public void call() {
            configurationOperations.clearPendingPlanChanges();
        }
    };

    @Inject
    UpgradeProgressOperations(ConfigurationOperations configurationOperations, PolicyOperations policyOperations) {
        this.configurationOperations = configurationOperations;
        this.policyOperations = policyOperations;
    }

    Observable<List<Urn>> awaitAccountUpgrade() {
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
