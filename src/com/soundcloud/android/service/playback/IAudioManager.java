package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import android.graphics.Bitmap;

public interface IAudioManager {

    public interface MusicFocusable {
        public void focusGained();
        public void focusLost(boolean isTransient, boolean canDuck);
    }

    int requestMusicFocus(MusicFocusable focusable);
    int abandonMusicFocus(boolean isTemporary);

    void setPlaybackState(State state);

    boolean isFocusSupported();
    boolean isTrackChangeSupported();

    void onFocusObtained();
    void onFocusAbandoned();
    void onTrackChanged(Track track, @Nullable Bitmap artwork);
}
