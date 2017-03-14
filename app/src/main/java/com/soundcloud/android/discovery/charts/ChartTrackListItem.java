package com.soundcloud.android.discovery.charts;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ChartType;

import java.util.Date;

abstract class ChartTrackListItem {

    enum Kind {
        CHART_HEADER,
        CHART_FOOTER,
        CHART_TRACK
    }

    abstract Kind kind();

    boolean isTrack() {
        return kind() == ChartTrackListItem.Kind.CHART_TRACK;
    }

    @AutoValue
    abstract static class Track extends ChartTrackListItem {
        abstract ChartTrackItem chartTrackItem();

        static ChartTrackListItem.Track create(ChartTrackItem chartTrackItem) {
            return new AutoValue_ChartTrackListItem_Track(Kind.CHART_TRACK, chartTrackItem);
        }
    }

    @AutoValue
    abstract static class Header extends ChartTrackListItem {
        abstract ChartType type();

        static ChartTrackListItem.Header create(ChartType type) {
            return new AutoValue_ChartTrackListItem_Header(Kind.CHART_HEADER, type);
        }
    }

    @AutoValue
    abstract static class Footer extends ChartTrackListItem {
        abstract Date lastUpdatedAt();

        static ChartTrackListItem.Footer create(Date lastUpdatedAt) {
            return new AutoValue_ChartTrackListItem_Footer(Kind.CHART_FOOTER, lastUpdatedAt);
        }
    }
}
