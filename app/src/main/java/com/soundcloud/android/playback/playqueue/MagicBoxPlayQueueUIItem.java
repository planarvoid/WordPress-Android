package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlayQueueManager;

class MagicBoxPlayQueueUIItem extends PlayQueueUIItem {

    MagicBoxPlayQueueUIItem(PlayState playState,
                            PlayQueueManager.RepeatMode repeatMode) {
        super(playState, repeatMode, false);
    }

    @Override
    Kind getKind() {
        return Kind.MAGIC_BOX;
    }

    @Override
    long getUniqueId() {
        return System.identityHashCode(Kind.MAGIC_BOX);
    }
}
