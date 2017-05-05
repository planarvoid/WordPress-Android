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

    private final FeatureFlags wrappedFlags;

    private FeatureFlagsHelper(FeatureFlags wrappedFlags) {
        this.wrappedFlags = wrappedFlags;
    }

    public static FeatureFlagsHelper create(Context context) {
        return new FeatureFlagsHelper(SoundCloudTestApplication.fromContext(context).getFeatureFlags());
    }

    public void enable(Flag flag) {
        wrappedFlags.setRuntimeFeatureFlagValue(flag, true);
    }

    public void disable(Flag flag) {
        wrappedFlags.setRuntimeFeatureFlagValue(flag, false);
    }

    public void assertEnabled(Flag... requiredEnabledFeatures) {
        checkState(isLocallyEnabled(requiredEnabledFeatures), "Required feature flags were not enabled. " + Arrays.toString(requiredEnabledFeatures));
    }

    public boolean isEnabled(Flag flag) {
        return wrappedFlags.isEnabled(flag);
    }

    public boolean isDisabled(Flag flag) {
        return wrappedFlags.isDisabled(flag);
    }

    public boolean isLocallyEnabled(Flag[] requiredEnabledFeatures) {
        for (Flag flag : requiredEnabledFeatures) {
            if (!wrappedFlags.getRuntimeFeatureFlagValue(flag)) {
                return false;
            }
        }
        return true;
    }

    public boolean isLocallyDisabled(Flag[] requiredDisabledFeatures) {
        for (Flag flag : requiredDisabledFeatures) {
            if (wrappedFlags.getRuntimeFeatureFlagValue(flag)) {
                return false;
            }
        }
        return true;
    }

    public void reset(Flag flag) {
        wrappedFlags.resetRuntimeFlagValue(flag);
    }
}
