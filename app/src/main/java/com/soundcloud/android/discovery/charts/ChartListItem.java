package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;

import java.util.List;

class ChartListItem {
    private final List<? extends ImageResource> trackArtworks;
    private final Urn genre;
    private final String displayName;
    private final ChartBucketType chartBucketType;
    private final ChartType chartType;
    private final ChartCategory chartCategory;

    ChartListItem(List<? extends ImageResource> trackArtworks,
                  Urn genre,
                  String displayName,
                  ChartBucketType chartBucketType,
                  ChartType chartType, ChartCategory chartCategory) {
        this.trackArtworks = trackArtworks;
        this.genre = genre;
        this.displayName = displayName;
        this.chartBucketType = chartBucketType;
        this.chartType = chartType;
        this.chartCategory = chartCategory;
    }

    List<? extends ImageResource> getTrackArtworks() {
        return trackArtworks;
    }

    Urn getGenre() {
        return genre;
    }

    String getDisplayName() {
        return displayName;
    }

    ChartBucketType getChartBucketType() {
        return chartBucketType;
    }

    ChartType getChartType() {
        return chartType;
    }

    ChartCategory getChartCategory() {
        return chartCategory;
    }
}
