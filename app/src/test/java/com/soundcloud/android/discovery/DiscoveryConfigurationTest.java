package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.experiments.StaticDiscoverContentExperiment;
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

    @Mock private StaticDiscoverContentExperiment staticDiscoverContentExperiment;
    @Mock private FeatureFlags featureFlags;

    private DiscoveryConfiguration discoveryConfiguration;

    @Before
    public void setUp() throws Exception {
        discoveryConfiguration = new DiscoveryConfiguration(staticDiscoverContentExperiment, featureFlags);
    }

    @Test
    public void useDiscoveryNavigationTargetWhenDiscoverBackendFeatureFlagIsEnabledAndStaticContentExperimentIsDisabled() {
        when(featureFlags.isEnabled(Flag.DISCOVER_BACKEND)).thenReturn(true);
        when(staticDiscoverContentExperiment.isEnabled()).thenReturn(false);

        assertThat(discoveryConfiguration.navigationTarget()).isInstanceOf(DiscoveryNavigationTarget.class);
    }

    @Test
    public void useOldDiscoveryNavigationTargetWhenDiscoverBackendFeatureFlagIsEnabledAndStaticContentExperimentIsEnabled() {
        when(featureFlags.isEnabled(Flag.DISCOVER_BACKEND)).thenReturn(true);
        when(staticDiscoverContentExperiment.isEnabled()).thenReturn(true);

        assertThat(discoveryConfiguration.navigationTarget()).isInstanceOf(OldDiscoveryNavigationTarget.class);
    }

    @Test
    public void useOldDiscoveryNavigationTargetWhenDiscoverBackendFeatureFlagIsDisabled() {
        when(featureFlags.isEnabled(Flag.DISCOVER_BACKEND)).thenReturn(false);

        assertThat(discoveryConfiguration.navigationTarget()).isInstanceOf(OldDiscoveryNavigationTarget.class);
    }
}
