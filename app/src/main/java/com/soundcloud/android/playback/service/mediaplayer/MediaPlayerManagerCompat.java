package com.soundcloud.android.playback.service.mediaplayer;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.utils.ErrorUtils;

import android.media.MediaPlayer;

import javax.inject.Inject;

@VisibleForTesting
public class MediaPlayerManagerCompat implements MediaPlayerManager {

    @Inject
    public MediaPlayerManagerCompat() {
        // no-op
    }

    public MediaPlayer create() {
        return new MediaPlayer();
    }

    public void stopAndReleaseAsync(MediaPlayer mediaPlayer) {
        stopAndReleaseInternal(mediaPlayer);
    }

    // TODO: still necessary to release in thread since we dropped Gingerbread?
    @Override
    public void stopAndRelease(MediaPlayer mediaPlayer) {
        stopAndReleaseInternal(mediaPlayer);
    }


    private void stopAndReleaseInternal(final MediaPlayer mediaPlayer) {
        new Thread() {
            @Override
            public void run() {
                try {
                    mediaPlayer.reset();
                    mediaPlayer.release();
                } catch (IllegalStateException ex) {
                    ErrorUtils.handleSilentException(ex);
                }
            }
        }.start();
    }
}
