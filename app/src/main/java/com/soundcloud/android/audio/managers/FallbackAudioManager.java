package com.soundcloud.android.audio.managers;

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
    public boolean isFocusSupported() {
        return false;
    }

    @Override
    public void onFocusObtained() {
    }

    @Override
    public void onFocusAbandoned() {
    }
}
