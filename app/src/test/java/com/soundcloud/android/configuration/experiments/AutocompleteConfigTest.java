package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.AutocompleteConfig.CONFIGURATION;
import static com.soundcloud.android.configuration.experiments.AutocompleteConfig.VARIANT_CONTROL_A;
import static com.soundcloud.android.configuration.experiments.AutocompleteConfig.VARIANT_CONTROL_B;
import static com.soundcloud.android.configuration.experiments.AutocompleteConfig.VARIANT_QUERIES_ONLY;
import static com.soundcloud.android.configuration.experiments.AutocompleteConfig.VARIANT_SHORTCUTS_AND_QUERIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AutocompleteConfigTest {
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private AutocompleteConfig autocompleteConfig;

    @Before
    public void setUp() throws Exception {
        autocompleteConfig = new AutocompleteConfig(experimentOperations, featureFlags);
    }

    @Test
    public void featureFlagDisabledExperimentControlA() throws Exception {
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(false);
        whenVariant(VARIANT_CONTROL_A);

        assertThat(autocompleteConfig.isEnabled()).isFalse();
    }

    @Test
    public void featureFlagDisabledExperimentControlB() throws Exception {
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(false);
        whenVariant(VARIANT_CONTROL_B);

        assertThat(autocompleteConfig.isEnabled()).isFalse();
    }

    @Test
    public void featureFlagDisabledExperimentQueriesOnly() throws Exception {
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(false);
        whenVariant(VARIANT_QUERIES_ONLY);

        assertThat(autocompleteConfig.isEnabled()).isTrue();
    }

    @Test
    public void featureFlagDisabledExperimentLocalAndQueries() throws Exception {
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(false);
        whenVariant(VARIANT_SHORTCUTS_AND_QUERIES);

        assertThat(autocompleteConfig.isEnabled()).isTrue();
    }

    @Test
    public void featureFlagEnabledExperimentControlA() throws Exception {
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(true);
        whenVariant(VARIANT_CONTROL_A);

        assertThat(autocompleteConfig.isEnabled()).isTrue();
    }

    @Test
    public void featureFlagEnabledExperimentControlB() throws Exception {
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(true);
        whenVariant(VARIANT_CONTROL_B);

        assertThat(autocompleteConfig.isEnabled()).isTrue();
    }

    @Test
    public void featureFlagEnabledExperimentQueriesOnly() throws Exception {
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(true);
        whenVariant(VARIANT_QUERIES_ONLY);

        assertThat(autocompleteConfig.isEnabled()).isTrue();
    }

    @Test
    public void featureFlagEnabledExperimentLocalAndQueries() throws Exception {
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(true);
        whenVariant(VARIANT_SHORTCUTS_AND_QUERIES);

        assertThat(autocompleteConfig.isEnabled()).isTrue();
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(CONFIGURATION)).thenReturn(variant);
    }
}
