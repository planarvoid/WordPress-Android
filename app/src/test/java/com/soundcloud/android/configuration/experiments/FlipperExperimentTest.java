package com.soundcloud.android.configuration.experiments;

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
public class FlipperExperimentTest {
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private FlipperExperiment config;

    @Before
    public void setUp() throws Exception {
        config = new FlipperExperiment(experimentOperations, featureFlags);
    }

    @Test
    public void enabledIfExperimentEnabled() throws Exception {
        whenVariant("flipper");

        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    public void disabledIfExperimentDisabled() throws Exception {
        whenVariant("skippy");

        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    public void enablesFlipperWithFFWhenNotInExperiment() {
        whenVariant("unknown");
        when(featureFlags.isEnabled(Flag.FLIPPER)).thenReturn(true);

        assertThat(config.isEnabled()).isTrue();
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(FlipperExperiment.CONFIGURATION)).thenReturn(variant);
    }

}
