package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class FlipperPreloadConfigurationTest {

    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private FlipperPreloadConfiguration configuration;

    @Before
    public void setUp() {
        when(featureFlags.isEnabled(Flag.FLIPPER_PRELOAD)).thenReturn(false);

        configuration = new FlipperPreloadConfiguration(experimentOperations, featureFlags);
    }

    @Test
    public void testNoVariantAndFeatureDisabled() {
        setPreconditions(Strings.EMPTY);

        assertThat(configuration.isEnabled()).isFalse();
    }

    @Test
    public void testNoVariantAndFeatureEnabled() {
        when(featureFlags.isEnabled(Flag.FLIPPER_PRELOAD)).thenReturn(true);

        setPreconditions(Strings.EMPTY);

        assertThat(configuration.isEnabled()).isTrue();
    }

    @Test
    public void testControlVariantAndFeatureDisabled() {
        setPreconditions(FlipperPreloadConfiguration.VARIANT_CONTROL);

        assertThat(configuration.isEnabled()).isFalse();
    }

    @Test
    public void testControlVariantAndFeatureEnabled() {
        when(featureFlags.isEnabled(Flag.FLIPPER_PRELOAD)).thenReturn(true);

        setPreconditions(FlipperPreloadConfiguration.VARIANT_CONTROL);

        assertThat(configuration.isEnabled()).isTrue();
    }

    @Test
    public void testFlipperVariant() {
        setPreconditions(FlipperPreloadConfiguration.VARIANT_FLIPPER);

        assertThat(configuration.isEnabled()).isTrue();
    }

    private void setPreconditions(String variant) {
        when(experimentOperations.getExperimentVariant(FlipperPreloadConfiguration.CONFIGURATION)).thenReturn(variant);
    }

}
