package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;

import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;
import java.util.Arrays;

@ActiveExperiment
public class StaticDiscoverContentExperiment {
    private static final String NAME = "static_discover_content_android";
    static final String VARIANT_CONTROL = "control";
    static final String VARIANT_STATIC_DISCOVER_CONTENT = "static_discover_content";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL,
                                    VARIANT_STATIC_DISCOVER_CONTENT));

    private final ExperimentOperations experimentOperations;

    @Inject
    StaticDiscoverContentExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean isEnabled() {
        return isStaticDiscoverContent();
    }

    private boolean isStaticDiscoverContent() {
        return VARIANT_STATIC_DISCOVER_CONTENT.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
