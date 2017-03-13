package com.soundcloud.android.image;

import static com.soundcloud.android.storage.StorageModule.IMAGE_CONFIG;
import static java.util.Collections.unmodifiableSet;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class ImageConfigurationStorage {

    private static final String KEY_SIZE_SPECS = "size_specs";

    private final SharedPreferences preferences;

    @Inject
    public ImageConfigurationStorage(@Named(IMAGE_CONFIG) SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void storeAvailableSizeSpecs(Collection<String> sizeSpecs) {
        preferences.edit().putStringSet(KEY_SIZE_SPECS, new HashSet<>(sizeSpecs)).apply();
    }

    public Collection<String> loadAvailableSizeSpecs() {
        return unmodifiableSet(preferences.getStringSet(KEY_SIZE_SPECS, Collections.emptySet()));
    }
}
