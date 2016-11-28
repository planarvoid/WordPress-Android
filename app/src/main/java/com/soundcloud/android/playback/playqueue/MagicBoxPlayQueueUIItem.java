package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlayQueueManager;

class MagicBoxPlayQueueUIItem extends PlayQueueUIItem {

    private boolean isAutoPlay;

    MagicBoxPlayQueueUIItem(PlayState playState,
                            PlayQueueManager.RepeatMode repeatMode,
                            boolean isAutoPlay) {
        super(playState, repeatMode);
        this.isAutoPlay = isAutoPlay;
    }

    @Override
    Kind getKind() {
        return Kind.MAGIC_BOX;
    }

    @Override
    long getUniqueId() {
        return System.identityHashCode(Kind.MAGIC_BOX);
    }

    void setAutoPlay(boolean isAutoPlay) {
        this.isAutoPlay = isAutoPlay;
    }

    boolean isAutoPlay() {
        return isAutoPlay;
    }

}
