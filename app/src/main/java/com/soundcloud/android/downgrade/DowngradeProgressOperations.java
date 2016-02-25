package com.soundcloud.android.downgrade;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.offline.ClearTrackDownloadsCommand;
import com.soundcloud.android.policies.PolicyOperations;
import rx.Observable;
import rx.functions.Action0;

import javax.inject.Inject;

class DowngradeProgressOperations {

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
    DowngradeProgressOperations(ConfigurationOperations configurationOperations,
                                PolicyOperations policyOperations,
                                ClearTrackDownloadsCommand clearTrackDownloadsCommand) {
        this.configurationOperations = configurationOperations;
        this.policyOperations = policyOperations;
        this.clearTrackDownloadsCommand = clearTrackDownloadsCommand;
    }

    Observable<Object> awaitAccountDowngrade() {
        return policyOperations.refreshedTrackPolicies()
                .flatMap(continueWith(clearTrackDownloadsCommand.toObservable(null)))
                .doOnCompleted(clearPendingPlanChangeFlags)
                .cast(Object.class);
    }
}
