package com.soundcloud.android.configuration.features;

import rx.Observable;

import javax.inject.Inject;
import java.util.Map;

public class FeatureOperations {

    public static final String OFFLINE_SYNC = "offline_sync";
    public static final String OFFLINE_SYNC_UPSELL = "offline_sync_upsell";

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

    public boolean isEnabled(String featureName, boolean defaultValue) {
        return featureStorage.isEnabled(featureName, defaultValue);
    }

    public Observable<Boolean> getUpdates(String name) {
        return featureStorage.getUpdates(name);
    }

}
