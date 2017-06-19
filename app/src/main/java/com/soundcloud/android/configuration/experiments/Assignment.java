package com.soundcloud.android.configuration.experiments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Collections;
import java.util.List;

public class Assignment {

    private static final Assignment EMPTY = new Assignment(Collections.emptyList());
    private final List<Layer> layers;
    private final Optional<String> commaSeparatedVariantIds;

    @JsonCreator
    public Assignment(@JsonProperty("layers") List<Layer> layers) {
        this.layers = Collections.unmodifiableList(layers);
        this.commaSeparatedVariantIds = this.layers.isEmpty() ?
                                        Optional.absent() :
                                        Optional.of(Strings.joinOn(",").join(Lists.transform(this.layers, Layer::getVariantId)));
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

    @JsonIgnore
    public Optional<String> commaSeparatedVariantIds() {
        return commaSeparatedVariantIds;
    }
}
