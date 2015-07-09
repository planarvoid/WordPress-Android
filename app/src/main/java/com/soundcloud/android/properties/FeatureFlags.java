package com.soundcloud.android.properties;

import android.content.SharedPreferences;
import android.content.res.Resources;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeatureFlags {

    public static final String FEATURE_PREFIX = "feature_";
    private final Resources resources;
    private final SharedPreferences sharedPreferences;

    @Inject
    public FeatureFlags(Resources resources, SharedPreferences sharedPreferences) {
        this.resources = resources;
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isEnabled(Flag flag) {
        return sharedPreferences.getBoolean(getPreferenceKey(flag),
                resources.getBoolean(flag.getId()));
    }

    public boolean isDisabled(Flag flag) {
        return !sharedPreferences.getBoolean(getPreferenceKey(flag),
                resources.getBoolean(flag.getId()));
    }

    public boolean resetAndGet(Flag flag) {
        final boolean defaultValue = resources.getBoolean(flag.getId());
        sharedPreferences.edit().putBoolean(getPreferenceKey(flag), defaultValue).apply();
        return defaultValue;
    }

    public String getPreferenceKey(Flag flag) {
        return FEATURE_PREFIX + flag.getId();
    }
}
