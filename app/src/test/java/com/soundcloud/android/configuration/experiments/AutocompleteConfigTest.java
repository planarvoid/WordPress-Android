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
import com.soundcloud.android.search.suggestions.SuggestionItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class AutocompleteConfigTest {

    private static final String QUERY_STRING = "test";
    @Mock private ExperimentOperations experimentOperations;
    @Mock private FeatureFlags featureFlags;

    private AutocompleteConfig autocompleteConfig;
    private ArrayList<SuggestionItem> suggestedItems;

    @Before
    public void setUp() throws Exception {
        autocompleteConfig = new AutocompleteConfig(experimentOperations, featureFlags);

        suggestedItems = new ArrayList<>(5);
        String test = "test";
        suggestedItems.add(SuggestionItem.forUser(PropertySet.create(), test));
        suggestedItems.add(SuggestionItem.forTrack(PropertySet.create(), test));
        suggestedItems.add(SuggestionItem.forUser(PropertySet.create(), test));
        suggestedItems.add(SuggestionItem.forUser(PropertySet.create(), test));
        suggestedItems.add(SuggestionItem.forTrack(PropertySet.create(), test));
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

    @Test
    public void filterControlA() throws Exception {
        whenVariant(VARIANT_CONTROL_A);

        assertThat(autocompleteConfig.filter(suggestedItems)).isEqualTo(suggestedItems);
    }

    @Test
    public void filterControlB() throws Exception {
        whenVariant(VARIANT_CONTROL_B);

        assertThat(autocompleteConfig.filter(suggestedItems)).isEqualTo(suggestedItems);
    }

    @Test
    public void filterQueriesOnly() throws Exception {
        whenVariant(VARIANT_QUERIES_ONLY);
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(true);

        assertThat(autocompleteConfig.filter(suggestedItems)).isEqualTo(Collections.emptyList());
    }

    @Test
    public void filterQueriesAndShortcuts() throws Exception {
        whenVariant(VARIANT_SHORTCUTS_AND_QUERIES);
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(true);

        ArrayList<SuggestionItem> expecteditems = new ArrayList<>(4);
        expecteditems.add(SuggestionItem.forUser(PropertySet.create(), QUERY_STRING));
        expecteditems.add(SuggestionItem.forTrack(PropertySet.create(), QUERY_STRING));
        expecteditems.add(SuggestionItem.forUser(PropertySet.create(), QUERY_STRING));
        expecteditems.add(SuggestionItem.forTrack(PropertySet.create(), QUERY_STRING));
        assertThat(autocompleteConfig.filter(suggestedItems)).isEqualTo(expecteditems);
    }

    @Test
    public void stillFiltersWhenFeatureFlagDisabled() throws Exception {
        whenVariant(VARIANT_QUERIES_ONLY);
        when(featureFlags.isEnabled(Flag.AUTOCOMPLETE)).thenReturn(false);

        assertThat(autocompleteConfig.filter(suggestedItems)).isEqualTo(Collections.emptyList());
    }

    private void whenVariant(String variant) {
        when(experimentOperations.getExperimentVariant(CONFIGURATION)).thenReturn(variant);
    }
}
