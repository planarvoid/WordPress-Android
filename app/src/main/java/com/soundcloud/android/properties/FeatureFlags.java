package com.soundcloud.android.properties;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeatureFlags {

    public static final String FEATURE_PREFIX = "feature_";
    private final SharedPreferences sharedPreferences;

    @Inject
    public FeatureFlags(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isEnabled(Flag flag) {
        return sharedPreferences.getBoolean(getPreferenceKey(flag), flag.getValue());
    }

    public boolean isDisabled(Flag flag) {
        return !sharedPreferences.getBoolean(getPreferenceKey(flag), flag.getValue());
    }

    public boolean resetAndGet(Flag flag) {
        final boolean defaultValue = flag.getValue();
        sharedPreferences.edit().putBoolean(getPreferenceKey(flag), defaultValue).apply();
        return defaultValue;
    }

    public String getPreferenceKey(Flag flag) {
        return FEATURE_PREFIX + flag.getName();
    }
}
