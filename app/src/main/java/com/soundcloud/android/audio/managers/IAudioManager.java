package com.soundcloud.android.audio.managers;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.State;
import org.jetbrains.annotations.Nullable;

import android.graphics.Bitmap;
import android.media.AudioManager;

public interface IAudioManager {
    int FOCUS_GAIN           = AudioManager.AUDIOFOCUS_GAIN;
    int FOCUS_TRANSIENT      = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
    int FOCUS_TRANSIENT_DUCK = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;


    public interface MusicFocusable {
        public void focusGained();
        public void focusLost(boolean isTransient, boolean canDuck);
    }

    boolean requestMusicFocus(MusicFocusable focusable, int durationHint);
    boolean abandonMusicFocus(boolean isTemporary);

    boolean isFocusSupported();

    void onFocusObtained();
    void onFocusAbandoned();
}
