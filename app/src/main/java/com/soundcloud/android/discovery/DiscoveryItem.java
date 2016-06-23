package com.soundcloud.android.discovery;

import com.soundcloud.java.functions.Predicate;

public class DiscoveryItem {

    public static Predicate<DiscoveryItem> byKind(final Kind kind) {
        return new Predicate<DiscoveryItem>() {
            public boolean apply(DiscoveryItem input) {
                return input.getKind() == kind;
            }
        };
    }

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
