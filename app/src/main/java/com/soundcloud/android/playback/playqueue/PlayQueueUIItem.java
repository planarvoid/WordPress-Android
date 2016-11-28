package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlayQueueManager;

abstract class PlayQueueUIItem {

    enum Kind {TRACK, HEADER}

    private PlayState playState;
    private PlayQueueManager.RepeatMode repeatMode;

    public PlayQueueUIItem(PlayState playState, PlayQueueManager.RepeatMode repeatMode) {
        this.playState = playState;
        this.repeatMode = repeatMode;
    }

    abstract Kind getKind();

    abstract long getUniqueId();

    boolean isTrack() {
        return getKind().equals(Kind.TRACK);
    }

    boolean isHeader() {
        return getKind().equals(Kind.HEADER);
    }

    public PlayState getPlayState() {
        return playState;
    }

    public void setPlayState(PlayState playState) {
        this.playState = playState;
    }

    public PlayQueueManager.RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(PlayQueueManager.RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }
}
