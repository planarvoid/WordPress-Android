package com.soundcloud.android.playback.service.managers;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlaybackState;
import com.soundcloud.android.playback.service.RemoteControlReceiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;

@SuppressWarnings("UnusedDeclaration")
public class FroyoRemoteAudioManager extends FroyoAudioManager implements IRemoteAudioManager {

    protected final Class<? extends BroadcastReceiver> RECEIVER = RemoteControlReceiver.class;
    protected final ComponentName receiverComponent;

    public FroyoRemoteAudioManager(final Context context) {
        super(context);
        receiverComponent = new ComponentName(context, RECEIVER);
    }

    @Override
    public void onFocusObtained() {
        super.onFocusObtained();
        getAudioManager().registerMediaButtonEventReceiver(receiverComponent);
    }

    @Override
    public void onFocusAbandoned() {
        super.onFocusAbandoned();
        getAudioManager().unregisterMediaButtonEventReceiver(receiverComponent);
    }

    @Override
    public boolean isTrackChangeSupported() {
        return false;
    }

    @Override
    public void onTrackChanged(Track track, Bitmap artwork) {
    }

    @Override
    public void setPlaybackState(PlaybackState playbackState) {
    }

}
