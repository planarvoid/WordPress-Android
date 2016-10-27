package com.soundcloud.android.discovery.charts;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class ChartBucket {

    public static ChartBucket create(List<Chart> global, List<Chart> featuredGenres) {
        return new AutoValue_ChartBucket(global, featuredGenres);
    }

    // This is a list of charts from all charts
    abstract List<Chart> getGlobal();

    // This is a list of charts from genre charts
    abstract List<Chart> getFeaturedGenres();
}
