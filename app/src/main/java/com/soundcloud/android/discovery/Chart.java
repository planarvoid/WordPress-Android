package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

class Chart {
    private final Long localId;
    private final ChartType type;
    private final ChartCategory category;
    private final String title;
    private final String page;
    private final Optional<Urn> genre;
    private List<ChartTrack> chartTracks;

    private Chart(Long localId, ChartType type,
                  ChartCategory category,
                  String title,
                  String page,
                  Optional<Urn> genre) {
        this.localId = localId;
        this.type = type;
        this.category = category;
        this.title = title;
        this.page = page;
        this.genre = genre;
    }

    static Chart fromPropertySet(final PropertySet propertyBindings) {
        return new Chart(propertyBindings.get(ChartsProperty.LOCAL_ID),
                         propertyBindings.get(ChartsProperty.CHART_TYPE),
                         propertyBindings.get(ChartsProperty.CHART_CATEGORY),
                         propertyBindings.get(ChartsProperty.TITLE),
                         propertyBindings.get(ChartsProperty.PAGE),
                         propertyBindings.get(ChartsProperty.GENRE));
    }

    Long getLocalId() {
        return localId;
    }

    ChartType getType() {
        return type;
    }

    ChartCategory getCategory() {
        return category;
    }

    String getTitle() {
        return title;
    }

    String getPage() {
        return page;
    }

    Optional<Urn> getGenre() {
        return genre;
    }

    List<ChartTrack> getChartTracks() {
        return chartTracks;
    }

    void setChartTracks(List<ChartTrack> chartTracks) {
        this.chartTracks = Collections.unmodifiableList(chartTracks);
    }
}
