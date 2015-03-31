package com.soundcloud.android.configuration.features;

import com.soundcloud.android.properties.ApplicationProperties;
import rx.Observable;

import javax.inject.Inject;
import java.util.Map;

public class FeatureOperations {

    private static final String OFFLINE_CONTENT = "offline_sync";
    private static final String OFFLINE_CONTENT_UPSELL = "offline_sync_upsell";

    private final FeatureStorage featureStorage;

    @Inject
    public FeatureOperations(ApplicationProperties appProperties, FeatureStorage featureStorage) {
        this.featureStorage = featureStorage;
        if (appProperties.isAlphaBuild()) {
            update(OFFLINE_CONTENT, true);
        }
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

    public boolean isOfflineContentEnabled() {
        return featureStorage.isEnabled(OFFLINE_CONTENT, false);
    }

    public boolean isOfflineContentUpsellEnabled() {
        return featureStorage.isEnabled(OFFLINE_CONTENT_UPSELL, false);
    }

    public Observable<Boolean> offlineContentEnabled() {
         return getUpdates(OFFLINE_CONTENT);
    }

    private Observable<Boolean> getUpdates(String name) {
        return featureStorage.getUpdates(name);
    }

}
