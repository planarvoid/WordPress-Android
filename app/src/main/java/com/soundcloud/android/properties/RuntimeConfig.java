package com.soundcloud.android.properties;

import static com.soundcloud.android.storage.StorageModule.FEATURES_FLAGS;

import com.soundcloud.android.storage.PersistentStorage;
import com.soundcloud.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Locale;

@Singleton
class RuntimeConfig {
    @VisibleForTesting
    static final String RUNTIME_FEATURE_FLAG_PREFIX = "runtime_feature_%s";

    private final PersistentStorage persistentStorage;

    @Inject
    RuntimeConfig(@Named(FEATURES_FLAGS) PersistentStorage persistentStorage) {
        this.persistentStorage = persistentStorage;
    }

    String getFlagKey(Flag featureFlag) {
        return String.format(Locale.US, RUNTIME_FEATURE_FLAG_PREFIX, featureFlag.featureName());
    }

    boolean getFlagValue(Flag flag) {
        return persistentStorage.getValue(getFlagKey(flag), flag.featureValue());
    }

    void setFlagValue(Flag flag, boolean value) {
        persistentStorage.persist(getFlagKey(flag), value);
    }

    void resetFlagValue(Flag flag) {
        persistentStorage.remove(getFlagKey(flag));
    }

    boolean containsFlagValue(Flag flag) {
        return persistentStorage.contains(getFlagKey(flag));
    }
}
