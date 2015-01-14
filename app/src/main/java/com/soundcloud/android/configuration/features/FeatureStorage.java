package com.soundcloud.android.configuration.features;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
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

    @SuppressWarnings("unchecked")
    public Map<String, Boolean> listFeatures() {
        return (Map<String, Boolean>) sharedPreferences.getAll();
    }

    public void updateFeature(Map<String, Boolean> features) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        for (Map.Entry<String, Boolean> feature : features.entrySet()) {
            edit.putBoolean(feature.getKey(), feature.getValue());
        }
        edit.apply();
    }

    public void updateFeature(String name, boolean enabled) {
        sharedPreferences.edit().putBoolean(name, enabled).apply();
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
