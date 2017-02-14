package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DynamicLinkSharingConfigTest extends AndroidUnitTest {
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private DynamicLinkSharingConfig config;

    @Before
    public void setUp() throws Exception {
        config = new DynamicLinkSharingConfig(experimentOperations, featureFlags);
    }

    @Test
    public void enabledIfFlagEnabled() throws Exception {
        when(featureFlags.isEnabled(Flag.DYNAMIC_LINK_SHARING)).thenReturn(true);
        whenVariant("nope");

        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    public void enabledIfExperimentEnabled() throws Exception {
        when(featureFlags.isEnabled(Flag.DYNAMIC_LINK_SHARING)).thenReturn(false);
        whenVariant("dynamic_link_sharing");

        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    public void disabledIfFlagAndExperimentDisabled() throws Exception {
        when(featureFlags.isEnabled(Flag.DYNAMIC_LINK_SHARING)).thenReturn(false);
        whenVariant("nope");

        assertThat(config.isEnabled()).isFalse();
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(DynamicLinkSharingConfig.CONFIGURATION)).thenReturn(variant);
    }
}
