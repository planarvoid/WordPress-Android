package com.soundcloud.android.likes;

class TrackLikesItem {

    private final Kind kind;

    TrackLikesItem(Kind kind) {
        this.kind = kind;
    }

    public enum Kind {
        HeaderItem,
        TrackItem
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isTrack() {
        return Kind.TrackItem.equals(kind);
    }
}
