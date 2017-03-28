package com.soundcloud.android.properties;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.tests.SoundCloudTestApplication;
import com.soundcloud.annotations.VisibleForTesting;

import android.content.Context;

import java.util.Arrays;

/**
 * Used only for testing, otherwise use injectable {@link FeatureFlags} class.
 */
@VisibleForTesting
public class FeatureFlagsHelper {

    private final FeatureFlags featureFlags;

    private FeatureFlagsHelper(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public static FeatureFlagsHelper create(Context context) {
        return new FeatureFlagsHelper(SoundCloudTestApplication.fromContext(context).getFeatureFlags());
    }

    public void enable(Flag flag) {
        featureFlags.setRuntimeFeatureFlagValue(flag, true);
    }

    public void disable(Flag flag) {
        featureFlags.setRuntimeFeatureFlagValue(flag, false);
    }

    public void assertEnabled(Flag... requiredEnabledFeatures) {
        checkState(isLocallyEnabled(requiredEnabledFeatures), "Required feature flags were not enabled. " + Arrays.toString(requiredEnabledFeatures));
    }

    public boolean isLocallyEnabled(Flag[] requiredEnabledFeatures) {
        for (Flag flag : requiredEnabledFeatures) {
            if (!featureFlags.getRuntimeFeatureFlagValue(flag)) {
                return false;
            }
        }
        return true;
    }

    public boolean isLocallyDisabled(Flag[] requiredDisabledFeatures) {
        for (Flag flag : requiredDisabledFeatures) {
            if (featureFlags.getRuntimeFeatureFlagValue(flag)) {
                return false;
            }
        }
        return true;
    }

    public void reset(Flag flag) {
        featureFlags.resetRuntimeFlagValue(flag);
    }
}
