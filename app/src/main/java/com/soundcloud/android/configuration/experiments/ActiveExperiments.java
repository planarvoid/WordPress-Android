package com.soundcloud.android.configuration.experiments;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class ActiveExperiments {

    private static final String[] LAYERS = { "android_listening" };
    private static final List<String> EXPERIMENTS = Arrays.asList(ShareButtonExperiment.NAME);

    @Inject
    ActiveExperiments() {}

    public String[] getRequestLayers() {
        return LAYERS;
    }

    public boolean isActive(String experimentName) {
        return EXPERIMENTS.contains(experimentName);
    }

}
