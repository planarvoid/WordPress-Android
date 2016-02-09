package com.soundcloud.android.upgrade;

import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

class UpgradeProgressOperations {

    private final ConfigurationOperations configurationOperations;
    private final PolicyOperations policyOperations;
    private final Func1<Configuration, Observable<List<Urn>>> refreshPolicies =
            new Func1<Configuration, Observable<List<Urn>>>() {
                @Override
                public Observable<List<Urn>> call(Configuration configuration) {
                    return policyOperations.refreshedTrackPolicies();
                }
            };

    @Inject
    UpgradeProgressOperations(ConfigurationOperations configurationOperations, PolicyOperations policyOperations) {
        this.configurationOperations = configurationOperations;
        this.policyOperations = policyOperations;
    }

    Observable<List<Urn>> awaitAccountUpgrade() {
        return configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER)
                .flatMap(refreshPolicies);
    }
}
