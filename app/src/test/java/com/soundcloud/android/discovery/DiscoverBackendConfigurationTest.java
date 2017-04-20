package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DiscoverBackendConfigurationTest {

    @Mock private FeatureFlags featureFlags;

    private DiscoverBackendConfiguration discoverBackendConfiguration;

    @Before
    public void setUp() throws Exception {
        discoverBackendConfiguration = new DiscoverBackendConfiguration(featureFlags);
    }

    @Test
    public void testDiscoverBackendFeatureEnabled() {
        when(featureFlags.isEnabled(Flag.DISCOVER_BACKEND)).thenReturn(true);

        assertThat(discoverBackendConfiguration.isEnabled()).isTrue();
    }
}
