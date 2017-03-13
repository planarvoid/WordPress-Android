package com.soundcloud.android.configuration.experiments;

import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class ExperimentConfiguration {

    public static ExperimentConfiguration fromName(String layerName, String experimentName, List<String> variations) {
        return builder()
                .pattern(false)
                .layerName(layerName)
                .name(experimentName)
                .variations(variations)
                .build();
    }

    public static ExperimentConfiguration fromPattern(String layerName, String experimentPattern) {
        return builder()
                .pattern(true)
                .layerName(layerName)
                .name(experimentPattern)
                .variations(Collections.emptyList())
                .build();
    }

    public static Builder builder() {
        return new AutoValue_ExperimentConfiguration.Builder();
    }

    public abstract String getLayerName();

    public abstract String getName();

    public abstract List<String> getVariations();

    public abstract boolean isPattern();

    public boolean matches(Layer layer) {
        if (layer.getLayerName().equals(getLayerName())) {
            if (isPattern()) {
                return layer.getExperimentName().matches(getName());
            } else {
                return layer.getExperimentName().equals(getName());
            }
        }
        return false;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder layerName(String layer);

        public abstract Builder name(String name);

        public abstract Builder variations(List<String> variations);

        public abstract Builder pattern(boolean pattern);

        public abstract ExperimentConfiguration build();
    }
}
