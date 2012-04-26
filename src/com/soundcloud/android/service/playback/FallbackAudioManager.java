package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;

import android.graphics.Bitmap;
import android.media.AudioManager;

public class FallbackAudioManager implements IAudioManager {
    @Override
    public int requestMusicFocus(MusicFocusable focusable) {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @Override
    public int abandonMusicFocus(boolean isTemporary) {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @Override
    public void applyRemoteMetadata(Track track, Bitmap bitmap) {
    }

    @Override
    public void setPlaybackState(boolean isPlaying) {
    }

    @Override
    public boolean isSupported() {
        return false;
    }
}
