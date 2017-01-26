package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;
import java.util.Arrays;

public class AutocompleteConfig {
    private static final String NAME = "search_query_autocomplete_android2";
    static final String VARIANT_CONTROL_A = "control_a";
    static final String VARIANT_CONTROL_B = "control_b";
    static final String VARIANT_SHORTCUTS_AND_QUERIES = "shortcuts_and_queries";
    static final String VARIANT_QUERIES_ONLY = "queries_only";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL_A,
                                    VARIANT_CONTROL_B,
                                    VARIANT_QUERIES_ONLY,
                                    VARIANT_SHORTCUTS_AND_QUERIES));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    AutocompleteConfig(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return isFeatureFlagEnabled() || isShortcutsAndQueriesVariant() || isQueriesOnlyVariant();
    }

    public boolean isShortcutsAndQueriesVariant() {
        return VARIANT_SHORTCUTS_AND_QUERIES.equals(getVariant());
    }

    public boolean isQueriesOnlyVariant() {
        return VARIANT_QUERIES_ONLY.equals(getVariant());
    }

    public boolean isFeatureFlagEnabled() {
        return featureFlags.isEnabled(Flag.AUTOCOMPLETE);
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
