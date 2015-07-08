package com.soundcloud.android.playback;

import android.media.AudioManager;

public interface IAudioManager {
    int FOCUS_GAIN           = AudioManager.AUDIOFOCUS_GAIN;
    int FOCUS_TRANSIENT      = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
    int FOCUS_TRANSIENT_DUCK = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;


    interface MusicFocusable {
        void focusGained();
        void focusLost(boolean isTransient, boolean canDuck);
    }

    boolean requestMusicFocus(final MusicFocusable focusable, int durationHint);
    boolean abandonMusicFocus(boolean isTemporary);

    boolean isFocusSupported();

    void onFocusObtained();
    void onFocusAbandoned();
}
