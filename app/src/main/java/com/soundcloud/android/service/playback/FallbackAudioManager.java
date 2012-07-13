package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;

import android.graphics.Bitmap;

public class FallbackAudioManager implements IAudioManager {
    @Override
    public boolean requestMusicFocus(MusicFocusable focusable, int durationHint) {
        return true;
    }

    @Override
    public boolean abandonMusicFocus(boolean isTemporary) {
        return true;
    }

    @Override
    public void setPlaybackState(State state) {
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
