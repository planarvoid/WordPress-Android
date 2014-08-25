package com.soundcloud.android.playback.service.mediaplayer;

import android.media.MediaPlayer;

public interface MediaPlayerManager {
    MediaPlayer create();
    void stopAndReleaseAsync(MediaPlayer mediaPlayer);
    void stopAndRelease(MediaPlayer mediaPlayer);
}
