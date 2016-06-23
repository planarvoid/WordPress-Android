package com.soundcloud.android.sync.charts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.List;

public class ApiChartBucket {

    private final List<ApiChart> global;
    private final List<ApiChart> featuredGenres;

    public ApiChartBucket(@JsonProperty("global") ModelCollection<ApiChart> global,
                          @JsonProperty("featuredGenres") ModelCollection<ApiChart> featuredGenres) {
        this.global = global.getCollection();
        this.featuredGenres = featuredGenres.getCollection();
    }

    public List<ApiChart> getGlobal() {
        return global;
    }

    public List<ApiChart> getFeaturedGenres() {
        return featuredGenres;
    }
}
