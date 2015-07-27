package com.soundcloud.android.configuration.experiments;

import com.soundcloud.java.collections.Lists;

import javax.inject.Inject;
import java.util.List;

class ActiveExperiments {

    private static final String[] LAYERS = { "android_listening" };
    private static final List<Integer> IDS = Lists.newArrayList(27);

    @Inject
    ActiveExperiments() {}

    public String[] getRequestLayers() {
        return LAYERS;
    }

    public boolean isActive(int experimentId) {
        return IDS.contains(experimentId);
    }

}
