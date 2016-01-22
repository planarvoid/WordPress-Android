package com.soundcloud.android.upgrade;

import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.policies.PolicyOperations;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.internal.util.UtilityFunctions;

import javax.inject.Inject;

class UpgradeProgressOperations {

    private static final Func1<Throwable, Boolean> JUST_FALSE = new Func1<Throwable, Boolean>() {
        @Override
        public Boolean call(Throwable throwable) {
            return false;
        }
    };
    private static final Func2<Boolean, Boolean, UpgradeResult> TO_UPGRADE_RESULT =
            new Func2<Boolean, Boolean, UpgradeResult>() {
        @Override
        public UpgradeResult call(Boolean configurationSuccess, Boolean policiesSuccess) {
            return new UpgradeResult(configurationSuccess, policiesSuccess);
        }
    };

    private final ConfigurationOperations configurationOperations;
    private final PolicyOperations policyOperations;

    @Inject
    UpgradeProgressOperations(ConfigurationOperations configurationOperations, PolicyOperations policyOperations) {
        this.configurationOperations = configurationOperations;
        this.policyOperations = policyOperations;
    }

    Observable<UpgradeResult> awaitAccountUpgrade() {
        return Observable.zip(
                configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER)
                        .map(UtilityFunctions.alwaysTrue())
                        .onErrorReturn(JUST_FALSE),
                policyOperations.updatedTrackPolicies()
                        .map(UtilityFunctions.alwaysTrue())
                        .onErrorReturn(JUST_FALSE),
                TO_UPGRADE_RESULT
        ).doOnNext(new Action1<UpgradeResult>() {
            @Override
            public void call(UpgradeResult upgradeResult) {
                if (upgradeResult.hasFailures()) {
                    // TODO: trigger daily update sync in background

                }
            }
        });
    }

    final static class UpgradeResult {
        final boolean configurationReceived;
        final boolean policiesUpdated;

        UpgradeResult(boolean configurationReceived, boolean policiesUpdated) {
            this.configurationReceived = configurationReceived;
            this.policiesUpdated = policiesUpdated;
        }

        boolean hasFailures() {
            return !(configurationReceived && policiesUpdated);
        }
    }
}
