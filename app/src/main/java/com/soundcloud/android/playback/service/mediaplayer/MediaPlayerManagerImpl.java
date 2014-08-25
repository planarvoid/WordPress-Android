package com.soundcloud.android.playback.service.mediaplayer;

import com.google.common.annotations.VisibleForTesting;

import android.media.MediaPlayer;

import javax.inject.Inject;

@VisibleForTesting
public class MediaPlayerManagerImpl implements MediaPlayerManager {

    @Inject
    public MediaPlayerManagerImpl() {
    }

    public MediaPlayer create() {
        return new MediaPlayer();
    }

    public void stopAndReleaseAsync(final MediaPlayer mediaPlayer) {
        new Thread() {
            @Override
            public void run() {
                stopAndRelease(mediaPlayer);
            }
        }.start();
    }

    public void stopAndRelease(MediaPlayer mediaPlayer) {
        mediaPlayer.reset();
        mediaPlayer.release();
    }
}
