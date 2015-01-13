package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.experiments.Assignment;
import com.soundcloud.android.experiments.Layer;

import java.util.List;

class Configuration {
    public final Assignment assignment;

    @JsonCreator
    public Configuration(@JsonProperty("experiments") List<Layer> experimentLayers) {
        assignment = new Assignment(experimentLayers);
    }
}
