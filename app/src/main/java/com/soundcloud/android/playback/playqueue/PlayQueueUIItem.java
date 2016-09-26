package com.soundcloud.android.playback.playqueue;

abstract class PlayQueueUIItem {

    enum Kind {TRACK, HEADER}

    abstract Kind getKind();

    abstract long getUniqueId();

    boolean isTrack() {
        return getKind().equals(Kind.TRACK);
    }

    boolean isHeader() {
        return getKind().equals(Kind.HEADER);
    }

}
