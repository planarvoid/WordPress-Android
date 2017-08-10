package com.soundcloud.android.configuration.experiments;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TopResultsConfigTest {
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private TopResultsBackendExperiments topResultsBackendExperiments;

    private TopResultsConfig topResultsConfig;

    @Before
    public void setUp() throws Exception {
        topResultsConfig = new TopResultsConfig(experimentOperations, featureFlags, topResultsBackendExperiments);
    }

    @Test
    public void disablesTopResultsForControlGroup1() {
        setPreconditions(false, TopResultsConfig.VARIANT_CONTROL1, false);

        assertThat(topResultsConfig.isEnabled()).isFalse();
    }

    @Test
    public void disablesTopResultsForControlGroup2() {
        setPreconditions(false, TopResultsConfig.VARIANT_CONTROL2, false);

        assertThat(topResultsConfig.isEnabled()).isFalse();
    }

    @Test
    public void enablesTopResultsWithFeatureFlagTurnedOn() {
        setPreconditions(true, TopResultsConfig.VARIANT_CONTROL1, false);

        assertThat(topResultsConfig.isEnabled()).isTrue();
    }

    @Test
    public void enablesTopResultsWithTopResultVariant() {
        setPreconditions(false, TopResultsConfig.VARIANT_TOP_RESULT, false);

        assertThat(topResultsConfig.isEnabled()).isTrue();
    }

    @Test
    public void enablesTopResultsWithFixedResultVariant() {
        setPreconditions(false, TopResultsConfig.VARIANT_FIXED_BUCKETS, false);

        assertThat(topResultsConfig.isEnabled()).isTrue();
    }

    @Test
    public void enablesTopResultsWithVariableResultVariant() {
        setPreconditions(false, TopResultsConfig.VARIANT_VARIABLE_BUCKETS, false);

        assertThat(topResultsConfig.isEnabled()).isTrue();
    }

    @Test
    public void enablesTopResultsWhenBackendExperimentsEnabled() {
        setPreconditions(false, TopResultsConfig.VARIANT_CONTROL1, true);

        assertThat(topResultsConfig.isEnabled()).isTrue();
    }

    @Test
    public void disablesTopResultsWhenBackendExperimentsDisabled() {
        setPreconditions(false, TopResultsConfig.VARIANT_CONTROL1, false);

        assertThat(topResultsConfig.isEnabled()).isFalse();
    }

    private void setPreconditions(boolean featureFlag, String variant, Boolean backendExperiementEnabled) {
        when(featureFlags.isEnabled(Flag.SEARCH_TOP_RESULTS)).thenReturn(featureFlag);
        when(experimentOperations.getExperimentVariant(TopResultsConfig.CONFIGURATION)).thenReturn(variant);
        when(topResultsBackendExperiments.topResultsEnabled()).thenReturn(backendExperiementEnabled);
    }
}
