package com.soundcloud.android.playback;

import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;

import android.graphics.Bitmap;

public interface IRemoteAudioManager extends IAudioManager {
    void setPlaybackState(boolean isSupposedToBePlayings);
    boolean isTrackChangeSupported();
    void onTrackChanged(PropertySet track, @Nullable Bitmap artwork);
}
