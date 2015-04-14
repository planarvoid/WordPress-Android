package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.configuration.features.Feature;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Configuration {

    public final List<Feature> features;
    public final UserPlan plan;
    public final Assignment assignment;
    public final DeviceManagement deviceManagement;

    @JsonCreator
    public Configuration(
            @JsonProperty("features") List<Feature> features,
            @JsonProperty("plan") UserPlan plan,
            @JsonProperty("experiments") List<Layer> experimentLayers,
            @JsonProperty("device_management") DeviceManagement deviceManagement) {
        this.features = Collections.unmodifiableList(features);
        this.plan = plan;
        this.assignment = experimentLayers == null ? Assignment.empty() : new Assignment(experimentLayers);
        this.deviceManagement = deviceManagement;
    }

    public Map<String, Boolean> getFeatureMap() {
        final HashMap<String, Boolean> map = new HashMap<>(features.size());
        for (Feature feature : features) {
            map.put(feature.name, feature.enabled);
        }
        return map;
    }

}
