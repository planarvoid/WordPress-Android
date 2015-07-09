package com.soundcloud.android.configuration;

import com.soundcloud.android.offline.OfflineServiceInitiator;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;

import javax.inject.Inject;

public class ConfigurationFeatureController {

    private final FeatureOperations featureOperations;
    private final OfflineServiceInitiator offlineServiceInitiator;
    private final Observable<Boolean> offlineSyncFeatureUpdatesObservable;

    @Inject
    public ConfigurationFeatureController(OfflineServiceInitiator offlineServiceInitiator,
                                          FeatureOperations featureOperations) {
        this.offlineServiceInitiator = offlineServiceInitiator;
        this.featureOperations = featureOperations;
        this.offlineSyncFeatureUpdatesObservable = featureOperations.offlineContentEnabled();
    }

    public void subscribe() {
        initialise();

        offlineSyncFeatureUpdatesObservable.subscribe(new OfflineSyncFeatureUpdatesSubscriber());
    }

    private void initialise() {
        if (featureOperations.isOfflineContentEnabled()) {
            offlineServiceInitiator.subscribe();
        }
    }

    private class OfflineSyncFeatureUpdatesSubscriber extends DefaultSubscriber<Boolean> {
        @Override
        public void onNext(Boolean enabled) {
            if (enabled) {
                offlineServiceInitiator.subscribe();
            } else {
                offlineServiceInitiator.unsubscribe();
            }
        }
    }
}
