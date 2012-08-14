package com.soundcloud.android.audio.managers;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.State;

import android.content.Context;
import android.graphics.Bitmap;

public class FallbackRemoteAudioManager extends FallbackAudioManager implements IRemoteAudioManager {

    public FallbackRemoteAudioManager(Context context) {
        super(context);
    }

    @Override
    public void setPlaybackState(State state) {
    }

    @Override
    public boolean isTrackChangeSupported() {
        return false;
    }

    @Override
    public void onTrackChanged(Track track, Bitmap artwork) {
    }
}
