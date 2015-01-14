package com.soundcloud.android.configuration.features;

import javax.inject.Inject;
import java.util.Map;

public class FeatureOperations {

    private final FeatureStorage featureStorage;

    @Inject
    public FeatureOperations(FeatureStorage featureStorage) {
        this.featureStorage = featureStorage;
    }

    public void update(Map<String, Boolean> features) {
        featureStorage.updateFeature(features);
    }

    public void update(String name, boolean value) {
        featureStorage.updateFeature(name, value);
    }

    public Map<String, Boolean> listFeatures() {
        return featureStorage.listFeatures();
    }

    public boolean isEnabled(String featureName, boolean defaultValue) {
        return featureStorage.isEnabled(featureName, defaultValue);
    }
}
