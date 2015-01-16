package com.soundcloud.android.configuration;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentController;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;

import javax.inject.Inject;

public class ConfigurationFeatureController {

    @VisibleForTesting static final String OFFLINE_SYNC = "offline_sync";

    private final OfflineContentController offlineContentController;
    private final FeatureOperations featureOperations;
    private final Observable<Boolean> offlineSyncFeatureUpdatesObservable;

    @Inject
    public ConfigurationFeatureController(OfflineContentController offlineContentController,
                                          FeatureOperations featureOperations) {
        this.offlineContentController = offlineContentController;
        this.featureOperations = featureOperations;
        this.offlineSyncFeatureUpdatesObservable = featureOperations.getUpdates(OFFLINE_SYNC);
    }

    public void subscribe() {
        initialise();

        offlineSyncFeatureUpdatesObservable.subscribe(new OfflineSyncFeatureUpdatesSubscriber());
    }

    private void initialise() {
        if (featureOperations.isEnabled(OFFLINE_SYNC, false)) {
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
