package com.soundcloud.android.discovery;

public class DiscoveryItem {

    static DiscoveryItem forRecommendedTracksFooter() {
        return new DiscoveryItem(Kind.RecommendedTracksFooterItem);
    }

    static DiscoveryItem forSearchItem() {
        return new DiscoveryItem(Kind.SearchItem);
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
