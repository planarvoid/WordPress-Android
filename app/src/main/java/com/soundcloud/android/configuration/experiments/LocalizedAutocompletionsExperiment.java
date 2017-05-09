package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.Arrays;

@ActiveExperiment
public class LocalizedAutocompletionsExperiment {
    private static final String NAME = "search_localized_autocomplete_android";

    private static final String VARIANT_CONTROL_1 = "control1";
    private static final String VARIANT_CONTROL_2 = "control2";
    private static final String VARIANT_1 = "localized_autocomplete";
    private static final String VARIANT_2 = "localized_autocomplete2";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, Arrays.asList(VARIANT_CONTROL_1, VARIANT_CONTROL_2, VARIANT_1, VARIANT_2));

    private final ExperimentOperations experimentOperations;

    @Inject
    LocalizedAutocompletionsExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public Optional<String> variantName() {
        final String variant = getVariant();
        if (VARIANT_1.equals(variant) || VARIANT_2.equals(variant)) {
            return Optional.fromNullable(variant);
        }
        return Optional.absent();
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
