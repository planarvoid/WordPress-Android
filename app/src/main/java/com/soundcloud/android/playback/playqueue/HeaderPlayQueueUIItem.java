package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlayQueueManager;

class HeaderPlayQueueUIItem extends PlayQueueUIItem {

    private final long id;
    private final String header;

    HeaderPlayQueueUIItem(PlayState playState,
                          PlayQueueManager.RepeatMode repeatMode,
                          boolean isRemoveable, long id, String header) {
        super(playState, repeatMode, isRemoveable);
        this.id = id;
        this.header = header;
    }

    @Override
    Kind getKind() {
        return Kind.HEADER;
    }

    @Override
    long getUniqueId() {
        return id;
    }

    public String getHeader() {
        return header;
    }
}
