package com.soundcloud.android.playback.service.managers;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlaybackState;
import org.jetbrains.annotations.Nullable;

import android.graphics.Bitmap;

public interface IRemoteAudioManager extends IAudioManager {
    void setPlaybackState(PlaybackState playbackState);
    boolean isTrackChangeSupported();
    void onTrackChanged(Track track, @Nullable Bitmap artwork);
}
