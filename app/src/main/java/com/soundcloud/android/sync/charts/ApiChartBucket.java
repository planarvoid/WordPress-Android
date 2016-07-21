package com.soundcloud.android.sync.charts;

import java.util.List;

public class ApiChartBucket {
    private final List<ApiChart<ApiImageResource>> charts;
    private int bucketType;

    public ApiChartBucket(List<ApiChart<ApiImageResource>> charts, int bucketType) {
        this.charts = charts;
        this.bucketType = bucketType;
    }

    public List<ApiChart<ApiImageResource>> getCharts() {
        return charts;
    }

    public int getBucketType() {
        return bucketType;
    }
}
