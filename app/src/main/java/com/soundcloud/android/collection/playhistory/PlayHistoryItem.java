package com.soundcloud.android.collection.playhistory;

class PlayHistoryItem {
    public enum Kind {
        PlayHistoryHeader,
        PlayHistoryTrack
    }

    private final Kind kind;

    PlayHistoryItem(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

}
