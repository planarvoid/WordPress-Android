package com.soundcloud.android.sync.charts;

import java.util.List;

public class ApiChartBucket {
    private final List<ApiChart> charts;
    private int bucketType;

    public ApiChartBucket(List<ApiChart> charts, int bucketType) {
        this.charts = charts;
        this.bucketType = bucketType;
    }

    public List<ApiChart> getCharts() {
        return charts;
    }

    public int getBucketType() {
        return bucketType;
    }
}
