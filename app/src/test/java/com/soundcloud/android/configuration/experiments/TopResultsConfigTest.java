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
public class TopResultsConfigTest {
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private TopResultsConfig topResultsConfig;

    @Before
    public void setUp() throws Exception {
        topResultsConfig = new TopResultsConfig(experimentOperations, featureFlags);
    }

    @Test
    public void disablesTopResultsForControlGroup1() {
        setPreconditions(false, TopResultsConfig.VARIANT_CONTROL1);

        assertThat(topResultsConfig.isEnabled()).isFalse();
    }

    @Test
    public void disablesTopResultsForControlGroup2() {
        setPreconditions(false, TopResultsConfig.VARIANT_CONTROL2);

        assertThat(topResultsConfig.isEnabled()).isFalse();
    }

    @Test
    public void enablesTopResultsWithFeatureFlagTurnedOn() {
        setPreconditions(true, TopResultsConfig.VARIANT_CONTROL1);

        assertThat(topResultsConfig.isEnabled()).isTrue();
    }

    @Test
    public void enablesTopResultsWithTopResultVariant() {
        setPreconditions(false, TopResultsConfig.VARIANT_TOP_RESULT);

        assertThat(topResultsConfig.isEnabled()).isTrue();
    }

    @Test
    public void enablesTopResultsWithFixedResultVariant() {
        setPreconditions(false, TopResultsConfig.VARIANT_FIXED_BUCKETS);

        assertThat(topResultsConfig.isEnabled()).isTrue();
    }

    @Test
    public void enablesTopResultsWithVariableResultVariant() {
        setPreconditions(false, TopResultsConfig.VARIANT_VARIABLE_BUCKETS);

        assertThat(topResultsConfig.isEnabled()).isTrue();
    }

    private void setPreconditions(boolean featureFlag, String variant) {
        when(featureFlags.isEnabled(Flag.SEARCH_TOP_RESULTS)).thenReturn(featureFlag);
        when(experimentOperations.getExperimentVariant(TopResultsConfig.CONFIGURATION)).thenReturn(variant);
    }
}
