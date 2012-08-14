package com.soundcloud.android.audio.managers;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.State;
import org.jetbrains.annotations.Nullable;

import android.graphics.Bitmap;

public interface IRemoteAudioManager extends IAudioManager {
    void setPlaybackState(State state);
    boolean isTrackChangeSupported();
    void onTrackChanged(Track track, @Nullable Bitmap artwork);
}
