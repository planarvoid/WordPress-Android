package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ChartType;

abstract class ChartTrackListItem {

    static ChartTrackListItem.Header forHeader(ChartType chartType) {
        return new Header(chartType);
    }

    static ChartTrackListItem.Footer forFooter(long lastUpdatedAt) {
        return new Footer(lastUpdatedAt);
    }

    static ChartTrackListItem.Track forTrack(ChartTrackItem trackItem) {
        return new Track(trackItem);
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
        final long lastUpdatedAt;

        private Footer(long lastUpdatedAt) {
            super(Kind.ChartFooter);
            this.lastUpdatedAt = lastUpdatedAt;
        }
    }
}
