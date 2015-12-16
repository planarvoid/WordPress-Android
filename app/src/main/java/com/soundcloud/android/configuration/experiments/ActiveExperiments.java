package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.properties.ApplicationProperties;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class ActiveExperiments {

    public static final String LISTENING_LAYER = "android_listening";
    public static final List<Experiment> ACTIVE_EXPERIMENTS =
            Collections.singletonList(StreamDesignExperiment.EXPERIMENT);

    private static final String[] ACTIVE_LAYERS = {LISTENING_LAYER};

    private ApplicationProperties applicationProperties;

    @Inject
    ActiveExperiments(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String[] getRequestLayers() {
        return ACTIVE_LAYERS;
    }

    public boolean isActive(String experimentName) {
        if (!applicationProperties.isDebugBuild()) {
            for (Experiment experiment : ACTIVE_EXPERIMENTS) {
                if (experiment.getName().equals(experimentName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
