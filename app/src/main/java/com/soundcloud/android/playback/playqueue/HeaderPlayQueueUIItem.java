package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.java.optional.Optional;

class HeaderPlayQueueUIItem extends PlayQueueUIItem {

    private final PlaybackContext playbackContext;
    private final Optional<String> contentTitle;

    HeaderPlayQueueUIItem(PlaybackContext playbackContext, Optional<String> contentTitle) {
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

    public PlaybackContext getPlaybackContext() {
        return playbackContext;
    }

    public Optional<String> getContentTitle() {
        return contentTitle;
    }
}
