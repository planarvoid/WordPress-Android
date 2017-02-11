package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class Configuration {

    @JsonCreator
    public static Configuration create(
            @JsonProperty("features") List<Feature> features,
            @JsonProperty("plan") UserPlan userPlan,
            @JsonProperty("experiments") List<Layer> experimentLayers,
            @JsonProperty("device_management") DeviceManagement deviceManagement,
            @JsonProperty("self_destruct") boolean selfDestruct,
            @JsonProperty("image_size_specs") List<String> imageSizeSpecs) {
        return new AutoValue_Configuration.Builder()
                .features(Collections.unmodifiableList(features))
                .userPlan(userPlan)
                .assignment(experimentLayers == null ? Assignment.empty() : new Assignment(experimentLayers))
                .deviceManagement(deviceManagement)
                .selfDestruct(selfDestruct)
                .imageSizeSpecs(imageSizeSpecs)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_Configuration.Builder()
                .features(Collections.emptyList())
                .userPlan(new UserPlan(Plan.FREE_TIER.planId, true, Optional.absent(), Collections.emptyList()))
                .assignment(Assignment.empty())
                .deviceManagement(new DeviceManagement(true, false))
                .selfDestruct(false)
                .imageSizeSpecs(Collections.emptyList());
    }

    public abstract List<Feature> getFeatures();

    public abstract UserPlan getUserPlan();

    public abstract Assignment getAssignment();

    public abstract DeviceManagement getDeviceManagement();

    public abstract boolean isSelfDestruct();

    public abstract List<String> getImageSizeSpecs();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder features(List<Feature> features);

        public abstract Builder userPlan(UserPlan userPlan);

        public abstract Builder assignment(Assignment assignment);

        public abstract Builder deviceManagement(DeviceManagement deviceManagement);

        public abstract Builder selfDestruct(boolean selfDestruct);

        public abstract Builder imageSizeSpecs(List<String> imageSizeSpecs);

        public abstract Configuration build();

    }
}
