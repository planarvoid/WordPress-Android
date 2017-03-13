package com.soundcloud.android.configuration.experiments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class Assignment {

    private static final Assignment EMPTY = new Assignment(Collections.emptyList());
    private final List<Layer> layers;

    @JsonCreator
    public Assignment(@JsonProperty("layers") List<Layer> layers) {
        this.layers = layers;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return layers.isEmpty();
    }

    public static Assignment empty() {
        return EMPTY;
    }

    @JsonIgnore
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(500);
        builder.append("Assignment: ").append(layers.size()).append(" layer(s)\n");
        for (Layer layer : layers) {
            builder.append(layer);
        }
        return builder.toString();
    }

}
