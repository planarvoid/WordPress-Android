package com.soundcloud.android.configuration.features;

import rx.Observable;

import javax.inject.Inject;
import java.util.Map;

public class FeatureOperations {

    private static final String OFFLINE_SYNC = "offline_sync";
    private static final String OFFLINE_SYNC_UPSELL = "offline_sync_upsell";

    private final FeatureStorage featureStorage;

    @Inject
    public FeatureOperations(FeatureStorage featureStorage) {
        this.featureStorage = featureStorage;
    }

    public void update(Map<String, Boolean> features) {
        featureStorage.update(features);
    }

    public void update(String name, boolean value) {
        featureStorage.update(name, value);
    }

    public Map<String, Boolean> list() {
        return featureStorage.list();
    }

    public boolean isOfflineSyncEnabled() {
        return featureStorage.isEnabled(OFFLINE_SYNC, false);
    }

    public boolean isOfflineSyncUpsellEnabled() {
        return featureStorage.isEnabled(OFFLINE_SYNC_UPSELL, false);
    }

    public Observable<Boolean> offlineSyncEnabled() {
         return getUpdates(OFFLINE_SYNC);
    }

    private Observable<Boolean> getUpdates(String name) {
        return featureStorage.getUpdates(name);
    }

}
