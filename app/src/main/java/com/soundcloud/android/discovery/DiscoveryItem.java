package com.soundcloud.android.discovery;

public abstract class DiscoveryItem {

    public enum Kind {
        TrackRecommendationItem, PlaylistTagsItem
    }

    private final Kind kind;

    protected DiscoveryItem(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
