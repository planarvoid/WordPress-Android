package com.soundcloud.android.configuration.experiments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.java.objects.MoreObjects;

public class Layer {

    private final String layerName;
    private final int experimentId;
    private final String experimentName;
    private final int variantId;
    private final String variantName;

    public Layer(@JsonProperty("layer_name") String layerName,
                 @JsonProperty("experiment_id") int experimentId,
                 @JsonProperty("experiment_name") String experimentName,
                 @JsonProperty("variant_id") int variantId,
                 @JsonProperty("variant_name") String variantName) {
        this.layerName = layerName;
        this.experimentId = experimentId;
        this.experimentName = experimentName;
        this.variantId = variantId;
        this.variantName = variantName;
    }


    public String getLayerName() {
        return layerName;
    }

    public int getExperimentId() {
        return experimentId;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public int getVariantId() {
        return variantId;
    }

    public String getVariantName() {
        return variantName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("layerName", layerName)
                .add("experimentId", experimentId)
                .add("experimentName", experimentName)
                .add("variantId", variantId)
                .add("variantName", variantName)
                .toString();
    }
}
