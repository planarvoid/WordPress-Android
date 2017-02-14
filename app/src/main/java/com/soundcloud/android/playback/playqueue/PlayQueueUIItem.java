package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlayQueueManager;

abstract class PlayQueueUIItem {

    enum Kind {TRACK, HEADER, MAGIC_BOX}

    private PlayState playState;
    private PlayQueueManager.RepeatMode repeatMode;
    private boolean isRemoveable;

    PlayQueueUIItem(PlayState playState, PlayQueueManager.RepeatMode repeatMode, boolean isRemoveable) {
        this.playState = playState;
        this.repeatMode = repeatMode;
        this.isRemoveable = isRemoveable;
    }

    abstract Kind getKind();

    abstract long getUniqueId();

    boolean isTrack() {
        return getKind().equals(Kind.TRACK);
    }

    boolean isHeader() {
        return getKind().equals(Kind.HEADER);
    }

    boolean isMagicBox() {
        return getKind().equals(Kind.MAGIC_BOX);
    }

    PlayState getPlayState() {
        return playState;
    }

    boolean isPlayingOrPaused() {
        return PlayState.PLAYING.equals(playState) || PlayState.PAUSED.equals(playState);
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

    public boolean isRemoveable() {
        return isRemoveable;
    }

    public void setRemoveable(boolean removeable) {
        isRemoveable = removeable;
    }
}
