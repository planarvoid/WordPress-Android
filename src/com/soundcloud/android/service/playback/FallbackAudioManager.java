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
    public void setPlaybackState(boolean isPlaying) {
    }

    @Override
    public boolean isFocusSupported() {
        return false;
    }

    @Override
    public boolean isTrackChangeSupported() {
        return false;
    }

    @Override
    public void onFocusObtained() {
    }

    @Override
    public void onFocusAbandoned() {
    }

    @Override
    public void onTrackChanged(Track track, Bitmap artwork) {
    }
}
