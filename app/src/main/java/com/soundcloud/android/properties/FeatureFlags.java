package com.soundcloud.android.properties;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeatureFlags {
    private final RemoteConfig remoteConfig;
    private final LocalConfig localConfig;
    private final RuntimeConfig runtimeConfig;

    @Inject
    public FeatureFlags(RemoteConfig remoteConfig,
                        LocalConfig localConfig,
                        RuntimeConfig runtimeConfig) {
        this.remoteConfig = remoteConfig;
        this.localConfig = localConfig;
        this.runtimeConfig = runtimeConfig;
    }

    public boolean isEnabled(Flag flag) {
        if (runtimeConfig.containsFlagValue(flag)) {
            return runtimeConfig.getFlagValue(flag);
        } else {
            return !flag.isUnderDevelopment() && isFlagEnabled(flag);
        }
    }

    public boolean isDisabled(Flag flag) {
        return !isEnabled(flag);
    }

    public void fetchRemoteFlags(Context context) {
        remoteConfig.fetchFeatureFlags(context);
    }

    private boolean isFlagEnabled(Flag flag) {
        final boolean localFlagValue = localConfig.getFlagValue(flag);
        return remoteConfig.getFlagValue(flag, localFlagValue);
    }

    /**
     * Local development: reset runtime feature flag.
     * @return default local compile time flag value.
     */
    public boolean resetRuntimeFlagValue(Flag flag) {
        runtimeConfig.resetFlagValue(flag);
        return localConfig.getFlagValue(flag);
    }

    /**
     * Local development: get runtime flag key.
     * @return a string representing feature flag key.
     */
    public String getRuntimeFeatureFlagKey(Flag flag) {
        return runtimeConfig.getFlagKey(flag);
    }

    /**
     * Local development: get runtime flag value
     */
    public boolean getRuntimeFeatureFlagValue(Flag flag) {
        return runtimeConfig.getFlagValue(flag);
    }

    /**
     * Local development: set runtime flag key and value
     */
    public void setRuntimeFeatureFlagValue(Flag flag, boolean value) {
        runtimeConfig.setFlagValue(flag, value);
    }
}
