package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class FlipperExperimentTest extends AndroidUnitTest {
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
        whenVariant("control");

        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    public void ignoresFeatureFlagIfInControlVariant() {
        whenVariant("control");
        when(featureFlags.isEnabled(Flag.FLIPPER)).thenReturn(true);

        assertThat(config.isEnabled()).isFalse();
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(FlipperExperiment.CONFIGURATION)).thenReturn(variant);
    }

}
