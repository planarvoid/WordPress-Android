package com.soundcloud.android.experiments;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;

class Assignment {

    private List<Layer> layers = Collections.emptyList();

    public List<Layer> getLayers() {
        return layers;
    }

    public void setLayers(List<Layer> layers) {
        this.layers = layers;
    }

    public boolean isEmpty() {
        return layers.isEmpty();
    }

    public static Assignment empty() {
        return new Assignment();
    }

    @JsonIgnore
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Experiment assignment: ").append(layers.size()).append(" layer(s)").append('\n');
        for (Layer layer : layers) {
            builder.append(layer);
        }
        return builder.toString();
    }

}
