package com.soundcloud.android.olddiscovery;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHomeScreenConfigurationTest {

    @Mock private FeatureOperations featureOperations;
    @Mock private FeatureFlags featureFlags;

    private DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;

    @Before
    public void setUp() throws Exception {
        defaultHomeScreenConfiguration = new DefaultHomeScreenConfiguration(featureOperations, featureFlags);
    }

    @Test
    public void testNewHomeFeatureEnabledWithFeatureFlag() {
        when(featureFlags.isEnabled(Flag.NEW_HOME)).thenReturn(true);

        assertThat(defaultHomeScreenConfiguration.isDiscoveryHome()).isTrue();
        assertThat(defaultHomeScreenConfiguration.isStreamHome()).isFalse();
    }

    @Test
    public void testNewHomeFeatureEnabled() {
        when(featureOperations.isNewHomeEnabled()).thenReturn(true);

        assertThat(defaultHomeScreenConfiguration.isDiscoveryHome()).isTrue();
        assertThat(defaultHomeScreenConfiguration.isStreamHome()).isFalse();
    }

    @Test
    public void testNewHomeFeatureDisabled() {
        when(featureOperations.isNewHomeEnabled()).thenReturn(false);

        assertThat(defaultHomeScreenConfiguration.isDiscoveryHome()).isFalse();
        assertThat(defaultHomeScreenConfiguration.isStreamHome()).isTrue();
    }
}
