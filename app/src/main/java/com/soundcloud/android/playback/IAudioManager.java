package com.soundcloud.android.playback;

import android.media.AudioManager;

public interface IAudioManager {
    int FOCUS_GAIN           = AudioManager.AUDIOFOCUS_GAIN;

    interface MusicFocusable {
        void focusGained();
        void focusLost(boolean isTransient, boolean canDuck);
    }

    boolean requestMusicFocus(final MusicFocusable focusable, int durationHint);
    boolean abandonMusicFocus();

    void onFocusObtained();
    void onFocusAbandoned();
}
