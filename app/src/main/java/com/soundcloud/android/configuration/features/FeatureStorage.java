package com.soundcloud.android.configuration.features;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureStorage {
    private final SharedPreferences sharedPreferences;

    @Inject
    public FeatureStorage(@Named("Features") SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isEnabled(final String featureName, final boolean defaultValue) {
        return sharedPreferences.getBoolean(featureName, defaultValue);
    }

    public List<Feature> listFeatures() {
        final Map<String, ?> featurePrefs = sharedPreferences.getAll();
        final List<Feature> features = new ArrayList<>(featurePrefs.size());
        for (Map.Entry<String, ?> entry : featurePrefs.entrySet()) {
            features.add(new Feature(entry.getKey(), (Boolean) entry.getValue()));
        }
        return features;
    }

    public void updateFeature(List<Feature> features) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        for (Feature feature : features) {
            edit.putBoolean(feature.name, feature.enabled);
        }
        edit.apply();
    }

    public void updateFeature(Feature feature) {
        sharedPreferences.edit().putBoolean(feature.name, feature.enabled).apply();
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
