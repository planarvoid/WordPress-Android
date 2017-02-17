package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class NewForYouConfigTest extends AndroidUnitTest {
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private NewForYouConfig config;

    @Before
    public void setUp() throws Exception {
        config = new NewForYouConfig(experimentOperations, featureFlags);
    }

    @Test
    public void enabledIfTopFlagEnabled() {
        when(featureFlags.isEnabled(Flag.NEW_FOR_YOU_FIRST)).thenReturn(true);
        whenVariant("nope");

        assertThat(config.isTopPositionEnabled()).isTrue();
    }

    @Test
    public void enabledIfTopExperimentEnabled() {
        when(featureFlags.isEnabled(Flag.NEW_FOR_YOU_FIRST)).thenReturn(false);
        whenVariant("new_for_you_top");

        assertThat(config.isTopPositionEnabled()).isTrue();
    }

    @Test
    public void disabledIfTopFlagAndTopExperimentDisabled() {
        when(featureFlags.isEnabled(Flag.NEW_FOR_YOU_FIRST)).thenReturn(false);
        whenVariant("nope");

        assertThat(config.isTopPositionEnabled()).isFalse();
    }

    @Test
    public void enabledIfSecondFlagEnabled() {
        when(featureFlags.isEnabled(Flag.NEW_FOR_YOU_SECOND)).thenReturn(true);
        whenVariant("nope");

        assertThat(config.isSecondPositionEnabled()).isTrue();
    }

    @Test
    public void enabledIfSecondExperimentEnabled() {
        when(featureFlags.isEnabled(Flag.NEW_FOR_YOU_SECOND)).thenReturn(false);
        whenVariant("new_for_you_under_recos");

        assertThat(config.isSecondPositionEnabled()).isTrue();
    }

    @Test
    public void disabledIfSecondFlagAndSecondExperimentDisabled() {
        when(featureFlags.isEnabled(Flag.NEW_FOR_YOU_SECOND)).thenReturn(false);
        whenVariant("nope");

        assertThat(config.isSecondPositionEnabled()).isFalse();
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(NewForYouConfig.CONFIGURATION)).thenReturn(variant);
    }
}
