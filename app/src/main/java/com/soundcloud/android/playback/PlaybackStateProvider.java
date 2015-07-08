package com.soundcloud.android.playback;

import javax.inject.Inject;

public class PlaybackStateProvider {

    @Inject
    public PlaybackStateProvider() {}

    public boolean isPlaying() {
        final PlaybackService instance = PlaybackService.instance;
        return instance != null && instance.isPlayerPlaying();
    }

    public boolean isSupposedToBePlaying() {
        final PlaybackService instance = PlaybackService.instance;
        return instance != null && instance.isPlaying();
    }
}
