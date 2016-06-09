package com.soundcloud.android.discovery;

public class DiscoveryItem {

    public static DiscoveryItem forRecommendedTracksFooter() {
        return new DiscoveryItem(Kind.RecommendedTracksFooterItem);
    }

    public enum Kind {
        RecommendedStationsItem,
        RecommendedTracksItem,
        RecommendedTracksFooterItem,
        PlaylistTagsItem,
        SearchItem,
        ChartItem,
        Empty
    }

    private final Kind kind;

    public DiscoveryItem(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

}
