package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import javax.inject.Inject;
import java.util.Arrays;

public class DiscoveryGoUpsellConfig {
    private static final String NAME = "android_go_upsell_in_discovery";

    static final String VARIANT_CONTROL = "control";
    static final String VARIANT_SHOW_IN_DISCOVERY = "show_in_discovery";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER,
                      NAME,
                      Arrays.asList(VARIANT_CONTROL,
                                    VARIANT_SHOW_IN_DISCOVERY));

    private final ExperimentOperations experimentOperations;

    @Inject
    DiscoveryGoUpsellConfig(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean isEnabled() {
        return isShowInDisoveryVariant();
    }

    private boolean isShowInDisoveryVariant() {
        return VARIANT_SHOW_IN_DISCOVERY.equals(getVariant());
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
