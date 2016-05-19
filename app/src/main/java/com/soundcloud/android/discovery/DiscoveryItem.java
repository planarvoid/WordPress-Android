package com.soundcloud.android.discovery;

public class DiscoveryItem {

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

    public static DiscoveryItem forRecommendedStationsBucket() {
        return new DiscoveryItem(Kind.StationRecommendationItem);
    }
}
