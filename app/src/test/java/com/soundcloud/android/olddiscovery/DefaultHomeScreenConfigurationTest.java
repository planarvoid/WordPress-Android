package com.soundcloud.android.olddiscovery;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHomeScreenConfigurationTest {

    @Mock private FeatureOperations featureOperations;

    private DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;

    @Before
    public void setUp() throws Exception {
        defaultHomeScreenConfiguration = new DefaultHomeScreenConfiguration(featureOperations);
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
