package com.soundcloud.android.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.storage.PersistentStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Locale;

@RunWith(MockitoJUnitRunner.class)
public class RuntimeConfigTest {

    private static final Flag FLAG = Flag.TEST_FEATURE;
    private static final String FLAG_KEY = String.format(Locale.US, RuntimeConfig.RUNTIME_FEATURE_FLAG_PREFIX, FLAG).toLowerCase();

    private RuntimeConfig runtimeConfig;

    @Mock private PersistentStorage flagsStorage;

    @Before
    public void setUp() {
        runtimeConfig = new RuntimeConfig(flagsStorage);
    }

    @Test
    public void shouldPersistFlagValueInStorage() {
        runtimeConfig.setFlagValue(FLAG, FLAG.featureValue());

        verify(flagsStorage).persist(FLAG_KEY, FLAG.featureValue());
    }

    @Test
    public void shouldFollowRemoteFeatureFlagNamingConventions() {
        final String runtimeFlagKey = runtimeConfig.getFlagKey(FLAG);

        assertThat(runtimeFlagKey).isEqualTo(FLAG_KEY);
    }

    @Test
    public void getFlagValueShouldReturnValueFromStorage() {
        when(flagsStorage.getValue(FLAG_KEY, true)).thenReturn(true);

        final boolean flagValue = runtimeConfig.getFlagValue(FLAG);

        assertThat(flagValue).isTrue();
        verify(flagsStorage).getValue(FLAG_KEY, true);
    }

    @Test
    public void resetFlagShouldRemoveFlagFromStorage() {
        runtimeConfig.resetFlagValue(FLAG);

        verify(flagsStorage).remove(FLAG_KEY);
    }

    @Test
    public void containsFlagShouldShouldUsePersistentStorage() {
        runtimeConfig.containsFlagValue(FLAG);

        verify(flagsStorage).contains(FLAG_KEY);
    }
}
