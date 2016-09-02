package com.soundcloud.android.collection.playhistory;

abstract class PlayHistoryItem {
    public enum Kind {
        PlayHistoryHeader,
        PlayHistoryTrack,
        PlayHistoryEmpty
    }

    abstract Kind getKind();

}
