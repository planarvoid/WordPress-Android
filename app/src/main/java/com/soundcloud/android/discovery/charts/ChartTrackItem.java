package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

public class ChartTrackItem extends TrackItem {
    private final ChartType chartType;
    private final ChartCategory chartCategory;
    private final Urn genre;
    private final Optional<Urn> queryUrn;

    public ChartTrackItem(ChartType chartType, ApiTrack apiTrack, ChartCategory chartCategory, Urn genre, Optional<Urn> queryUrn) {
        this(chartType, apiTrack.toPropertySet(), chartCategory, genre, queryUrn);
    }

    @VisibleForTesting
    public ChartTrackItem(ChartType chartType,
                          PropertySet propertySet,
                          ChartCategory chartCategory,
                          Urn genre,
                          Optional<Urn> queryUrn) {
        super(propertySet);
        this.chartType = chartType;
        this.chartCategory = chartCategory;
        this.genre = genre;
        this.queryUrn = queryUrn;
    }

    public ChartType chartType() {
        return chartType;
    }

    ChartCategory chartCategory() {
        return chartCategory;
    }

    Urn genre() {
        return genre;
    }

    Optional<Urn> queryUrn() {
        return queryUrn;
    }
}
