package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;

import android.graphics.Bitmap;

public interface IAudioManager {

    public interface MusicFocusable {
        public void focusGained();
        public void focusLost(boolean isTransient, boolean canDuck);
    }

    int requestMusicFocus(MusicFocusable focusable);
    int abandonMusicFocus(boolean isTemporary);

    void applyRemoteMetadata(final Track track, final Bitmap bitmap);
    void setPlaybackState(boolean isPlaying);

    boolean isSupported();
}
