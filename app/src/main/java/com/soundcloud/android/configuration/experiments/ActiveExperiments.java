package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.properties.ApplicationProperties;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

class ActiveExperiments {

    private static final String[] LAYERS = {"android_listening"};

    private static final List<String> EXPERIMENTS = Collections.singletonList(
            StreamDesignExperiment.NAME
    );

    private ApplicationProperties applicationProperties;

    @Inject
    ActiveExperiments(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String[] getRequestLayers() {
        return LAYERS;
    }

    public boolean isActive(String experimentName) {
        return EXPERIMENTS.contains(experimentName) && !applicationProperties.isDebugBuild();
    }

}
