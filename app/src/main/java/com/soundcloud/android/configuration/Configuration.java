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
            @JsonProperty("features") List<Feature> features,
            @JsonProperty("experiments") List<Layer> experimentLayers) {
        this.features = Collections.unmodifiableList(features);
        this.assignment = experimentLayers == null ? Assignment.empty() : new Assignment(experimentLayers);
    }
}
