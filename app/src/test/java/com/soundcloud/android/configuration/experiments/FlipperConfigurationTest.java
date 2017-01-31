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
public class FlipperConfigurationTest {

    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private FlipperConfiguration flipperConfiguration;

    @Before
    public void setUp() {
        when(featureFlags.isEnabled(Flag.FLIPPER)).thenReturn(false);

        flipperConfiguration = new FlipperConfiguration(experimentOperations, featureFlags);
    }

    @Test
    public void testNoVariantAndFeatureDisabled() {
        setPreconditions(Strings.EMPTY);

        assertThat(flipperConfiguration.isEnabled()).isFalse();
    }

    @Test
    public void testNoVariantAndFeatureEnabled() {
        when(featureFlags.isEnabled(Flag.FLIPPER)).thenReturn(true);

        setPreconditions(Strings.EMPTY);

        assertThat(flipperConfiguration.isEnabled()).isTrue();
    }

    @Test
    public void testControlVariantAndFeatureDisabled() {
        setPreconditions(FlipperConfiguration.VARIANT_CONTROL);

        assertThat(flipperConfiguration.isEnabled()).isFalse();
    }

    @Test
    public void testControlVariantAndFeatureEnabled() {
        when(featureFlags.isEnabled(Flag.FLIPPER)).thenReturn(true);

        setPreconditions(FlipperConfiguration.VARIANT_CONTROL);

        assertThat(flipperConfiguration.isEnabled()).isTrue();
    }

    @Test
    public void testFlipperVariant() {
        setPreconditions(FlipperConfiguration.VARIANT_FLIPPER);

        assertThat(flipperConfiguration.isEnabled()).isTrue();
    }

    private void setPreconditions(String variant) {
        when(experimentOperations.getExperimentVariant(FlipperConfiguration.CONFIGURATION)).thenReturn(variant);
    }

}
