package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.configuration.features.Feature;

import java.util.Collections;
import java.util.List;

class Configuration {
    public final Assignment assignment;
    public final List<Feature> features;

    @JsonCreator
    public Configuration(
            @JsonProperty("experiments") List<Layer> experimentLayers,
            @JsonProperty("features") List<Feature> features) {
        this.features = Collections.unmodifiableList(features);
        this.assignment = new Assignment(experimentLayers);
    }
}
