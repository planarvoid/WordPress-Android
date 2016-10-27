package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.api.model.ChartType;

import java.util.Date;

abstract class ChartTrackListItem {

    static ChartTrackListItem.Header forHeader(ChartType chartType) {
        return new Header(chartType);
    }

    static ChartTrackListItem.Footer forFooter(Date lastUpdatedAt) {
        return new Footer(lastUpdatedAt);
    }

    static ChartTrackListItem.Track forTrack(ChartTrackItem track) {
        return new Track(track);
    }

    enum Kind {
        ChartHeader, ChartFooter, TrackItem
    }

    private final Kind kind;

    private ChartTrackListItem(Kind kind) {
        this.kind = kind;
    }

    Kind getKind() {
        return kind;
    }

    static class Track extends ChartTrackListItem {
        final ChartTrackItem chartTrackItem;

        private Track(ChartTrackItem chartTrackItem) {
            super(Kind.TrackItem);
            this.chartTrackItem = chartTrackItem;
        }
    }

    static class Header extends ChartTrackListItem {
        final ChartType type;

        private Header(ChartType type) {
            super(Kind.ChartHeader);
            this.type = type;
        }
    }

    static class Footer extends ChartTrackListItem {
        final Date lastUpdatedAt;

        private Footer(Date lastUpdatedAt) {
            super(Kind.ChartFooter);
            this.lastUpdatedAt = lastUpdatedAt;
        }
    }
}
