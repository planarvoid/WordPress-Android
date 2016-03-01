package com.soundcloud.android.configuration;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.offline.ClearTrackDownloadsCommand;
import com.soundcloud.android.policies.PolicyOperations;
import rx.Observable;
import rx.functions.Action0;

import javax.inject.Inject;

public class PlanChangeOperations {

    private final ConfigurationOperations configurationOperations;
    private final PolicyOperations policyOperations;
    private final PlaybackServiceInitiator playbackServiceInitiator;
    private final ClearTrackDownloadsCommand clearTrackDownloadsCommand;
    private final Action0 clearPendingPlanChangeFlags = new Action0() {
        @Override
        public void call() {
            configurationOperations.clearPendingPlanChanges();
        }
    };
    private final Action0 resetPlaybackService = new Action0() {
        @Override
        public void call() {
            playbackServiceInitiator.resetPlaybackService();
        }
    };

    @Inject
    PlanChangeOperations(ConfigurationOperations configurationOperations,
                         PolicyOperations policyOperations,
                         PlaybackServiceInitiator playbackServiceInitiator, ClearTrackDownloadsCommand clearTrackDownloadsCommand) {
        this.configurationOperations = configurationOperations;
        this.policyOperations = policyOperations;
        this.playbackServiceInitiator = playbackServiceInitiator;
        this.clearTrackDownloadsCommand = clearTrackDownloadsCommand;
    }

    public Observable<Object> awaitAccountDowngrade() {
        return configurationOperations.awaitConfigurationFromPendingPlanChange()
                .compose(new PlanChangedSteps<Configuration>())
                .doOnCompleted(clearTrackDownloadsCommand.toAction0());
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
        return updatedConfiguration.compose(new PlanChangedSteps<Configuration>());
    }

    private final class PlanChangedSteps<T> implements Observable.Transformer<T, Object> {

        @Override
        public Observable<Object> call(Observable<T> source) {
            return source.flatMap(continueWith(policyOperations.refreshedTrackPolicies()))
                    .doOnSubscribe(resetPlaybackService)
                    .doOnCompleted(clearPendingPlanChangeFlags)
                    .cast(Object.class);
        }
    }
}
