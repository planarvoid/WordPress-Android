package com.soundcloud.android.discovery;

abstract class DiscoveryItem {

    enum Kind {
        TrackRecommendationItem, PlaylistTagsItem
    }

    private final Kind kind;

    DiscoveryItem(Kind kind) {
        this.kind = kind;
    }

    Kind getKind() {
        return kind;
    }
}
