package com.soundcloud.android.olddiscovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.functions.Predicate;

public abstract class OldDiscoveryItem {

    public static Predicate<OldDiscoveryItem> byKind(final Kind kind) {
        return input -> input.getKind() == kind;
    }

    public static OldDiscoveryItem forRecommendedTracksFooter() {
        return Default.create(Kind.RecommendedTracksFooterItem);
    }

    static OldDiscoveryItem forSearchItem() {
        return Default.create(Kind.SearchItem);
    }

    public enum Kind {
        NewForYouItem,
        RecommendedStationsItem,
        RecommendedTracksItem,
        RecommendedTracksFooterItem,
        RecommendedPlaylistsItem,
        PlaylistTagsItem,
        SearchItem,
        ChartItem,
        RecentlyPlayedItem,
        WelcomeUserItem,
        Empty,
        UpsellItem
    }

    public abstract Kind getKind();

    @AutoValue
    abstract static class Default extends OldDiscoveryItem {
        public static OldDiscoveryItem create(Kind kind) {
            return new AutoValue_OldDiscoveryItem_Default(kind);
        }
    }
}
