package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.java.optional.Optional;

class HeaderPlayQueueUIItem extends PlayQueueUIItem {

    private final PlaybackContext playbackContext;
    private final Optional<String> contentTitle;

    HeaderPlayQueueUIItem(PlaybackContext playbackContext,
                          Optional<String> contentTitle,
                          PlayState playState,
                          PlayQueueManager.RepeatMode repeatMode) {
        super(playState, repeatMode);
        this.playbackContext = playbackContext;
        this.contentTitle = contentTitle;
    }

    @Override
    Kind getKind() {
        return Kind.HEADER;
    }

    @Override
    long getUniqueId() {
        return System.identityHashCode(playbackContext);
    }

    PlaybackContext getPlaybackContext() {
        return playbackContext;
    }

    Optional<String> getContentTitle() {
        return contentTitle;
    }

}
