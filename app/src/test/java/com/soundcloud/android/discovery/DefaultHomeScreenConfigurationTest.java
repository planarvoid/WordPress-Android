package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHomeScreenConfigurationTest {

    @Mock private PlaylistDiscoveryConfig playlistDiscoveryConfig;
    @Mock private FeatureOperations featureOperations;

    private DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;

    @Before
    public void setUp() throws Exception {
        defaultHomeScreenConfiguration = new DefaultHomeScreenConfiguration(playlistDiscoveryConfig, featureOperations);
    }

    @Test
    public void testNewHomeExperimentEnabled() {
        when(playlistDiscoveryConfig.isEnabled()).thenReturn(true);

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
    public void testFeatureAndExperimentDisabled() {
        assertThat(defaultHomeScreenConfiguration.isDiscoveryHome()).isFalse();
        assertThat(defaultHomeScreenConfiguration.isStreamHome()).isTrue();
    }
}
