package com.soundcloud.android.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.SharedPreferences;

@RunWith(MockitoJUnitRunner.class)
public class FeatureFlagsTest {

    private static final String FLAG_KEY = FeatureFlags.FEATURE_PREFIX + Flag.TEST_FEATURE.getName();

    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;

    private FeatureFlags featureFlags;

    @Before
    public void setUp() throws Exception {
        featureFlags = new FeatureFlags(sharedPreferences);
    }

    @Test
    public void isEnabledShouldUseResourceValueAsDefaultValue() {
        when(sharedPreferences.getBoolean(FLAG_KEY, true)).thenReturn(true);

        assertThat(featureFlags.isEnabled(Flag.TEST_FEATURE)).isTrue();

        verify(sharedPreferences).getBoolean(FLAG_KEY, true);
    }

    @Test
    public void isDisabledShouldUseResourceValueAsDefaultValue() {
        when(sharedPreferences.getBoolean(FLAG_KEY, true)).thenReturn(true);

        assertThat(featureFlags.isDisabled(Flag.TEST_FEATURE)).isFalse();

        verify(sharedPreferences).getBoolean(FLAG_KEY, true);
    }

    @Test
    public void resetShouldResetValueToResourceValueAndReturnDefaultValue() {
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putBoolean(FLAG_KEY, true)).thenReturn(sharedPreferencesEditor);

        assertThat(featureFlags.resetAndGet(Flag.TEST_FEATURE)).isTrue();

        InOrder inOrder = Mockito.inOrder(sharedPreferencesEditor);
        inOrder.verify(sharedPreferencesEditor).putBoolean(FLAG_KEY, true);
        inOrder.verify(sharedPreferencesEditor).apply();
    }
}