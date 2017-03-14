package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryGoUpsellConfigTest {

    @Mock ExperimentOperations experimentOperations;

    private DiscoveryGoUpsellConfig config;

    @Before
    public void setUp() throws Exception {
        config = new DiscoveryGoUpsellConfig(experimentOperations);
    }

    @Test
    public void checkControl() throws Exception {
        when(experimentOperations.getExperimentVariant(DiscoveryGoUpsellConfig.CONFIGURATION)).thenReturn(DiscoveryGoUpsellConfig.VARIANT_CONTROL);

        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    public void checkVariant() throws Exception {
        when(experimentOperations.getExperimentVariant(DiscoveryGoUpsellConfig.CONFIGURATION)).thenReturn(DiscoveryGoUpsellConfig.VARIANT_SHOW_IN_DISCOVERY);

        assertThat(config.isEnabled()).isTrue();
    }
}
