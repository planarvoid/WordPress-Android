package com.soundcloud.android.playback.service.mediaplayer;

import android.media.MediaPlayer;

import javax.inject.Inject;

public class MediaPlayerManager {

    @Inject
    public MediaPlayerManager() {
        // no-op
    }

    public MediaPlayer create() {
        return new MediaPlayer();
    }

    public void stopAndReleaseAsync(final MediaPlayer mediaPlayer) {
        new Thread() {
            @Override
            public void run() {
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        }.start();

    }
}
