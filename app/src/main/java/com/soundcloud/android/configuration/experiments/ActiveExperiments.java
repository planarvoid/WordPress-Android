package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.properties.ApplicationProperties;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class ActiveExperiments {

    public static final String LISTENING_LAYER = "android_listening";
    public static final List<ExperimentConfiguration> ACTIVE_EXPERIMENTS = Arrays.asList(ShareAsTextButtonExperiment.CONFIGURATION, StationsRecoAlgorithmExperiment.CONFIGURATION);

    private static final String[] ACTIVE_LAYERS = {LISTENING_LAYER};

    private ApplicationProperties applicationProperties;

    @Inject
    ActiveExperiments(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String[] getRequestLayers() {
        return ACTIVE_LAYERS;
    }

    public boolean isActive(Layer layer) {
        if (!applicationProperties.isDebugBuild()) {
            for (ExperimentConfiguration experiment : ACTIVE_EXPERIMENTS) {
                if (experiment.matches(layer)) {
                    return true;
                }
            }
        }
        return false;
    }

}
