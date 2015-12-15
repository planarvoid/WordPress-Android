package com.soundcloud.android.configuration.experiments;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class Experiment {

    public static Experiment create(String layerName, String name, List<String> variations) {
        return builder()
                .layerName(layerName)
                .name(name)
                .variations(variations)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_Experiment.Builder();
    }

    public abstract String getLayerName();

    public abstract String getName();

    public abstract List<String> getVariations();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder layerName(String layer);

        public abstract Builder name(String name);

        public abstract Builder variations(List<String> variations);

        public abstract Experiment build();
    }
}
