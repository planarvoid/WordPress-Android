package com.soundcloud.android.playback.service.managers;

import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import android.graphics.Bitmap;

public interface IRemoteAudioManager extends IAudioManager {
    void setPlaybackState(boolean isSupposedToBePlayings);
    boolean isTrackChangeSupported();
    void onTrackChanged(Track track, @Nullable Bitmap artwork);
}
