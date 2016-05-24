package com.soundcloud.android.sync.charts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ModelCollection;

public class ApiChartBucket {
    private final ModelCollection<ApiChart> global;
    private final ModelCollection<ApiChart> featuredGenres;

    public ApiChartBucket(@JsonProperty("global") ModelCollection<ApiChart> global,
                          @JsonProperty("featuredGenres") ModelCollection<ApiChart> featuredGenres) {
        this.global = global;
        this.featuredGenres = featuredGenres;
    }

    public ModelCollection<ApiChart> getGlobal() {
        return global;
    }

    public ModelCollection<ApiChart> getFeaturedGenres() {
        return featuredGenres;
    }
}
