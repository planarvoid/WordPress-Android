package com.soundcloud.android.discovery;

public abstract class DiscoveryItem {

    public enum Kind {
        StationRecommendationItem, TrackRecommendationItem, PlaylistTagsItem, SearchItem, ChartItem
    }

    private final Kind kind;

    public DiscoveryItem(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
