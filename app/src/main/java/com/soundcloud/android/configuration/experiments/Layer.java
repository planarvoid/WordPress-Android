package com.soundcloud.android.configuration.experiments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Layer {

    private String layerName;
    private int experimentId;
    private String experimentName;
    private int variantId;
    private String variantName;

    @JsonProperty("layer_name")
    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    @JsonProperty("experiment_id")
    public int getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(int experimentId) {
        this.experimentId = experimentId;
    }

    @JsonProperty("experiment_name")
    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    @JsonProperty("variant_id")
    public int getVariantId() {
        return variantId;
    }

    public void setVariantId(int variantId) {
        this.variantId = variantId;
    }

    @JsonProperty("variant_name")
    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    @JsonIgnore
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append(layerName).append('\n')
                .append(experimentName).append(':').append(experimentId).append('\n')
                .append(variantName).append(':').append(variantId).append('\n');
        return builder.toString();
    }

}
