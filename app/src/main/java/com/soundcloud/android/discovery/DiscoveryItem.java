package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.functions.Predicate;

public abstract class DiscoveryItem {

    public static Predicate<DiscoveryItem> byKind(final Kind kind) {
        return new Predicate<DiscoveryItem>() {
            public boolean apply(DiscoveryItem input) {
                return input.getKind() == kind;
            }
        };
    }

    public static DiscoveryItem forRecommendedTracksFooter() {
        return Default.create(Kind.RecommendedTracksFooterItem);
    }

    static DiscoveryItem forSearchItem() {
        return Default.create(Kind.SearchItem);
    }

    public enum Kind {
        RecommendedStationsItem,
        RecommendedTracksItem,
        RecommendedTracksFooterItem,
        RecommendedPlaylistsItem,
        PlaylistTagsItem,
        SearchItem,
        ChartItem,
        RecentlyPlayedItem,
        Empty
    }

    public abstract Kind getKind();

    @AutoValue
    abstract static class Default extends DiscoveryItem {
        public static DiscoveryItem create(Kind kind) {
            return new AutoValue_DiscoveryItem_Default(kind);
        }
    }
}
