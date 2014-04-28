package com.soundcloud.android.properties;

import android.content.res.Resources;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeatureFlags {

    private final Resources resources;

    @Inject
    public FeatureFlags(Resources resources) {
        this.resources = resources;
    }

    public boolean isEnabled(Feature feature) {
        return resources.getBoolean(feature.getId());
    }

    public boolean isDisabled(Feature feature) {
        return !isEnabled(feature);
    }

}
