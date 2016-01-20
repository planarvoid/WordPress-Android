package com.soundcloud.android.payments;

import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.policies.PolicyOperations;
import rx.Observable;
import rx.internal.util.UtilityFunctions;

import javax.inject.Inject;

class UpgradeProgressOperations {

    private ConfigurationOperations configurationOperations;
    private PolicyOperations policyOperations;

    @Inject
    UpgradeProgressOperations(ConfigurationOperations configurationOperations, PolicyOperations policyOperations) {
        this.configurationOperations = configurationOperations;
        this.policyOperations = policyOperations;
    }

    Observable<Object> awaitAccountUpgrade() {
        return Observable.zip(
                configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER),
                policyOperations.updatedTrackPolicies(),
                UtilityFunctions.returnNull()
        );
    }

}
