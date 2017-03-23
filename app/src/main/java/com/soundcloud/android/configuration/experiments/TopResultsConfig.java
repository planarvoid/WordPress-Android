package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;
import static java.util.Arrays.asList;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;

@ActiveExperiment
public class TopResultsConfig {
    private static final String NAME = "topresults_on_android";
    static final String VARIANT_CONTROL1 = "control1";
    static final String VARIANT_CONTROL2 = "control2";
    static final String VARIANT_TOP_RESULT = "topresult";
    static final String VARIANT_FIXED_BUCKETS = "fixed_buckets";
    static final String VARIANT_VARIABLE_BUCKETS = "variable_buckets";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, asList(VARIANT_CONTROL1, VARIANT_CONTROL2, VARIANT_TOP_RESULT, VARIANT_FIXED_BUCKETS, VARIANT_VARIABLE_BUCKETS));

    private final ExperimentOperations experimentOperations;
    private final FeatureFlags featureFlags;

    @Inject
    TopResultsConfig(ExperimentOperations experimentOperations, FeatureFlags featureFlags) {
        this.experimentOperations = experimentOperations;
        this.featureFlags = featureFlags;
    }

    public boolean isEnabled() {
        return featureFlags.isEnabled(Flag.SEARCH_TOP_RESULTS) || isExperimentEnabled();
    }

    private boolean isExperimentEnabled() {
        switch(getVariant()) {
            case VARIANT_TOP_RESULT:
            case VARIANT_FIXED_BUCKETS:
            case VARIANT_VARIABLE_BUCKETS:
                return true;
            default:
                return false;
        }
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
