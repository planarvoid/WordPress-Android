package com.soundcloud.android.sync.charts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.storage.Tables;

import java.util.ArrayList;
import java.util.List;

public class ApiFeaturedCharts {

    private final List<ApiChartBucket> apiChartBuckets;

    public ApiFeaturedCharts(@JsonProperty("global") ModelCollection<ApiChart> global,
                             @JsonProperty("featuredGenres") ModelCollection<ApiChart> featuredGenres) {
        apiChartBuckets = new ArrayList<>(2);
        apiChartBuckets.add(new ApiChartBucket(global.getCollection(), Tables.Charts.BUCKET_TYPE_GLOBAL));
        apiChartBuckets.add(new ApiChartBucket(featuredGenres.getCollection(),
                                               Tables.Charts.BUCKET_TYPE_FEATURED_GENRES));
    }

    public List<ApiChartBucket> getApiChartBuckets() {
        return apiChartBuckets;
    }
}
