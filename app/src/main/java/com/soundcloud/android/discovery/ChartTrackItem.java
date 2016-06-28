package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.tracks.TrackItem;

abstract class ChartTrackItem {

    static ChartTrackItem.Header forHeader(ChartType chartType) {
        return new Header(chartType);
    }

    static ChartTrackItem.Footer forFooter(long lastUpdatedAt) {
        return new Footer(lastUpdatedAt);
    }

    static ChartTrackItem.Track forTrack(TrackItem trackItem) {
        return new Track(trackItem);
    }

    enum Kind {
        ChartHeader, ChartFooter, TrackItem
    }

    private final Kind kind;

    private ChartTrackItem(Kind kind) {
        this.kind = kind;
    }

    Kind getKind() {
        return kind;
    }

    static class Track extends ChartTrackItem {
        final TrackItem trackItem;

        private Track(TrackItem trackItem) {
            super(Kind.TrackItem);
            this.trackItem = trackItem;
        }
    }

    static class Header extends ChartTrackItem {
        final ChartType type;

        private Header(ChartType type) {
            super(Kind.ChartHeader);
            this.type = type;
        }
    }

    static class Footer extends ChartTrackItem {
        final long lastUpdatedAt;

        private Footer(long lastUpdatedAt) {
            super(Kind.ChartFooter);
            this.lastUpdatedAt = lastUpdatedAt;
        }
    }
}
