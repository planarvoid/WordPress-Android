package com.soundcloud.android.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;

@RunWith(MockitoJUnitRunner.class)
public class FeatureFlagsTest {

    private static final Flag FEATURE_FLAG = Flag.TEST_FEATURE;
    private static final Flag FEATURE_FLAG_UNDER_DEVELOPMENT = Flag.TEST_FEATURE_UNDER_DEVELOPMENT;

    @Mock private RemoteConfig remoteConfig;
    @Mock private LocalConfig localConfig;
    @Mock private RuntimeConfig runtimeConfig;
    @Mock private Context context;

    private FeatureFlags featureFlags;

    @Before
    public void setUp() {
        featureFlags = new FeatureFlags(remoteConfig, localConfig, runtimeConfig);
    }

    @Test
    public void shouldFetchRemoteFlags() {
        featureFlags.fetchRemoteFlags(context);

        verify(remoteConfig).fetchFeatureFlags(context);
    }

    @Test
    public void shouldBeDisabledIfFeatureIsUnderDevelopment() {
        assertThat(featureFlags.isEnabled(FEATURE_FLAG_UNDER_DEVELOPMENT)).isFalse();
        assertThat(featureFlags.isDisabled(FEATURE_FLAG_UNDER_DEVELOPMENT)).isTrue();
    }

    @Test
    public void shouldReturnLocalConfigValueIfThereIsNoRemoteConfigValue() {
        final boolean localValue = FEATURE_FLAG.featureValue();

        when(localConfig.getFlagValue(FEATURE_FLAG)).thenReturn(localValue);
        when(remoteConfig.getFlagValue(FEATURE_FLAG, localValue)).thenReturn(localValue);

        assertThat(featureFlags.isEnabled(FEATURE_FLAG)).isEqualTo(localValue);
    }

    @Test
    public void shouldReturnRemoteConfigValueIfThereIsNoOverriddenConfigValue() {
        final boolean localValue = FEATURE_FLAG.featureValue();

        when(localConfig.getFlagValue(FEATURE_FLAG)).thenReturn(localValue);
        when(remoteConfig.getFlagValue(FEATURE_FLAG, localValue)).thenReturn(false);

        assertThat(featureFlags.isEnabled(FEATURE_FLAG)).isFalse();
    }

    @Test
    public void isEnabledShouldReturnRuntimeConfigValueIfThereIsFlagOverriddenValue() {
        when(runtimeConfig.containsFlagValue(FEATURE_FLAG)).thenReturn(true);
        when(runtimeConfig.getFlagValue(FEATURE_FLAG)).thenReturn(true);

        assertThat(featureFlags.isEnabled(FEATURE_FLAG)).isEqualTo(true);
        verifyZeroInteractions(remoteConfig);
    }

    @Test
    public void isDisabledShouldReturnRuntimeConfigValueIfThereIsFlagOverriddenValue() {
        when(runtimeConfig.containsFlagValue(FEATURE_FLAG)).thenReturn(true);
        when(runtimeConfig.getFlagValue(FEATURE_FLAG)).thenReturn(false);

        assertThat(featureFlags.isDisabled(FEATURE_FLAG)).isEqualTo(true);
        verifyZeroInteractions(remoteConfig);
    }

    @Test
    public void shouldGetRuntimeFeatureFlagKey() {
        when(runtimeConfig.getFlagKey(FEATURE_FLAG)).thenReturn(FEATURE_FLAG.featureName());

        assertThat(featureFlags.getRuntimeFeatureFlagKey(FEATURE_FLAG)).isEqualTo(FEATURE_FLAG.featureName());
    }

    @Test
    public void shouldSetRuntimeFeatureFlagValue() {
        featureFlags.setRuntimeFeatureFlagValue(FEATURE_FLAG, FEATURE_FLAG.featureValue());

        verify(runtimeConfig).setFlagValue(FEATURE_FLAG, FEATURE_FLAG.featureValue());
    }

    @Test
    public void shouldResetRuntimeFlagValueAndReturnDefaultLocalValue() {
        when(localConfig.getFlagValue(FEATURE_FLAG)).thenReturn(FEATURE_FLAG.featureValue());

        assertThat(featureFlags.resetRuntimeFlagValue(FEATURE_FLAG)).isTrue();
        verify(runtimeConfig).resetFlagValue(FEATURE_FLAG);
    }
}
