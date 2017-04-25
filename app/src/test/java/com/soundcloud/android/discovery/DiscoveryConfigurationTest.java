package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.olddiscovery.OldDiscoveryNavigationTarget;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryConfigurationTest {

    @Mock private FeatureFlags featureFlags;

    private DiscoveryConfiguration discoverBackendConfiguration;

    @Before
    public void setUp() throws Exception {
        discoverBackendConfiguration = new DiscoveryConfiguration(featureFlags);
    }

    @Test
    public void useDiscoveryNavigationTargetWhenDiscoverBackendIsEnabled() {
        when(featureFlags.isEnabled(Flag.DISCOVER_BACKEND)).thenReturn(true);

        assertThat(discoverBackendConfiguration.navigationTarget()).isInstanceOf(DiscoveryNavigationTarget.class);
    }

    @Test
    public void useOldDiscoveryNavigationTargetWhenDiscoverBackendIsDisabled() {
        when(featureFlags.isEnabled(Flag.DISCOVER_BACKEND)).thenReturn(false);

        assertThat(discoverBackendConfiguration.navigationTarget()).isInstanceOf(OldDiscoveryNavigationTarget.class);
    }
}
