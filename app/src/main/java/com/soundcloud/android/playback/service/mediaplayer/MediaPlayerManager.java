package com.soundcloud.android.playback.service.mediaplayer;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.utils.ErrorUtils;

import android.media.MediaPlayer;

import javax.inject.Inject;

@VisibleForTesting
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
                stopAndRelease(mediaPlayer);
            }
        }.start();
    }

    public void stopAndRelease(MediaPlayer mediaPlayer) {
        try {
            mediaPlayer.reset();
            mediaPlayer.release();
        } catch (IllegalStateException ex) {
            ErrorUtils.handleSilentException(ex);
        }
    }
}
