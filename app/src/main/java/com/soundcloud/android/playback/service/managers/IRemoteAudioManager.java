package com.soundcloud.android.playback.service.managers;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import org.jetbrains.annotations.Nullable;

import android.graphics.Bitmap;

public interface IRemoteAudioManager extends IAudioManager {
    void setPlaybackState(boolean isSupposedToBePlayings);
    boolean isTrackChangeSupported();
    void onTrackChanged(PublicApiTrack track, @Nullable Bitmap artwork);
}
