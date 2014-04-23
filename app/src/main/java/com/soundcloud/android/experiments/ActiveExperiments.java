package com.soundcloud.android.experiments;

import com.google.common.collect.Lists;

import javax.inject.Inject;
import java.util.List;

class ActiveExperiments {

    private static final String[] LAYERS = { "android-aa" };
    private static final List<Integer> IDS = Lists.newArrayList( 1 );

    @Inject
    ActiveExperiments() {}

    public String[] getRequestLayers() {
        return LAYERS;
    }

    public boolean isActive(int experimentId) {
        return IDS.contains(experimentId);
    }

}
