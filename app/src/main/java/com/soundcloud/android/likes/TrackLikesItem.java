package com.soundcloud.android.likes;

abstract class TrackLikesItem {

    public enum Kind {
        HeaderItem,
        TrackItem
    }

    public abstract Kind getKind();

    public boolean isTrack() {
        return Kind.TrackItem.equals(getKind());
    }
}
