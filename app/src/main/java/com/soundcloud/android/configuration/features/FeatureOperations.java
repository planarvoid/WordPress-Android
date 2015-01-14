package com.soundcloud.android.configuration.features;

import javax.inject.Inject;
import java.util.List;

public class FeatureOperations {

    private final FeatureStorage featureStorage;

    @Inject
    public FeatureOperations(FeatureStorage featureStorage) {
        this.featureStorage = featureStorage;
    }

    public void update(List<Feature> features) {
        featureStorage.updateFeature(features);
    }

    public void update(Feature feature) {
        featureStorage.updateFeature(feature);
    }

    public List<Feature> listFeatures() {
        return featureStorage.listFeatures();
    }

    public boolean isEnabled(String featureName, boolean defaultValue) {
        return featureStorage.isEnabled(featureName, defaultValue);
    }
}
