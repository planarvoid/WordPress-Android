package com.soundcloud.android.configuration;

import com.soundcloud.android.offline.OfflineContentController;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;

import javax.inject.Inject;

public class ConfigurationFeatureController {

    private final FeatureOperations featureOperations;
    private final OfflineContentController offlineContentController;
    private final Observable<Boolean> offlineSyncFeatureUpdatesObservable;

    @Inject
    ConfigurationFeatureController(OfflineContentController offlineContentController,
                                   FeatureOperations featureOperations) {
        this.offlineContentController = offlineContentController;
        this.featureOperations = featureOperations;
        this.offlineSyncFeatureUpdatesObservable = featureOperations.offlineContentEnabled();
    }

    public void subscribe() {
        initialise();

        offlineSyncFeatureUpdatesObservable.subscribe(new OfflineSyncFeatureUpdatesSubscriber());
    }

    private void initialise() {
        if (featureOperations.isOfflineContentEnabled()) {
            offlineContentController.subscribe();
        }
    }

    private class OfflineSyncFeatureUpdatesSubscriber extends DefaultSubscriber<Boolean> {
        @Override
        public void onNext(Boolean enabled) {
            if (enabled) {
                offlineContentController.subscribe();
            } else {
                offlineContentController.unsubscribe();
            }
        }
    }
}
