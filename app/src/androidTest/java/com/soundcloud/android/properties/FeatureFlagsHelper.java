package com.soundcloud.android.properties;

import static com.soundcloud.android.storage.StorageModule.PREFS_FEATURE_FLAGS;
import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.storage.PersistentStorage;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import com.soundcloud.annotations.VisibleForTesting;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.Arrays;

/**
 * {@link FeatureFlags} decorator to avoid increasing scope and visibility of
 * {@link FeatureFlags} class for testing purpose. <br>
 *
 * Used only for testing, otherwise use injectable {@link FeatureFlags} class.
 */
@VisibleForTesting
public class FeatureFlagsHelper {

    private final RuntimeConfig runtimeConfig;

    private FeatureFlagsHelper(Context context) {
        final PersistentStorage persistentStorage = new PersistentStorage(createPreferencesForFeatureFlags(context));
        runtimeConfig = new RuntimeConfig(persistentStorage);
    }

    public static FeatureFlagsHelper create(Context context) {
        return new FeatureFlagsHelper(context);
    }

    public void enable(Flag flag) {
        runtimeConfig.setFlagValue(flag, true);
    }

    public void disable(Flag flag) {
        runtimeConfig.setFlagValue(flag, false);
    }

    public void assertIsEnabled(Flag... requiredEnabledFeatures) {
        checkState(isLocallyDisabled(requiredEnabledFeatures), "Required feature flags were not enabled. " + Arrays.toString(requiredEnabledFeatures));
    }

    public boolean isLocallyEnabled(Flag[] requiredEnabledFeatures) {
        for (Flag flag : requiredEnabledFeatures) {
            if (!runtimeConfig.getFlagValue(flag)) {
                return false;
            }
        }
        return true;
    }

    public boolean isLocallyDisabled(Flag[] requiredDisabledFeatures) {
        for (Flag flag : requiredDisabledFeatures) {
            if (runtimeConfig.getFlagValue(flag)) {
                return false;
            }
        }
        return true;
    }

    public void reset(Flag flag) {
        runtimeConfig.resetFlagValue(flag);
    }

    @NonNull
    private SharedPreferences createPreferencesForFeatureFlags(Context context) {
        return new ObfuscatedPreferences(context.getSharedPreferences(PREFS_FEATURE_FLAGS, Context.MODE_PRIVATE), new Obfuscator());
    }

}
