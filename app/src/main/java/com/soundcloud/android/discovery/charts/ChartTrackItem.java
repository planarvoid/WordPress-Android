package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public class ChartTrackItem {
    private final ChartType chartType;
    private final TrackItem trackItem;
    private final ChartCategory chartCategory;
    private final Urn genre;
    private final Optional<Urn> queryUrn;

    public ChartTrackItem(ChartType chartType, ApiTrack apiTrack, ChartCategory chartCategory, Urn genre, Optional<Urn> queryUrn) {
        this(chartType, TrackItem.from(apiTrack), chartCategory, genre, queryUrn);
    }

    private ChartTrackItem(ChartType chartType,
                          TrackItem trackItem,
                          ChartCategory chartCategory,
                          Urn genre,
                          Optional<Urn> queryUrn) {
        this.chartType = chartType;
        this.trackItem = trackItem;
        this.chartCategory = chartCategory;
        this.genre = genre;
        this.queryUrn = queryUrn;
    }

    public ChartType chartType() {
        return chartType;
    }

    public TrackItem getTrackItem() {
        return trackItem;
    }

    public Urn getUrn() {
        return trackItem.getUrn();
    }

    public Date getCreatedAt() {
        return trackItem.getCreatedAt();
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
