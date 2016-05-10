package com.soundcloud.android.discovery;

abstract class DiscoveryItem {

    enum Kind {
        StationRecommendationItem, TrackRecommendationItem, PlaylistTagsItem, SearchItem
    }

    private final Kind kind;

    DiscoveryItem(Kind kind) {
        this.kind = kind;
    }

    Kind getKind() {
        return kind;
    }
}
