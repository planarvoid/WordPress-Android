package com.soundcloud.android.audio.managers;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.RemoteControlReceiver;
import com.soundcloud.android.service.playback.State;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;

@SuppressWarnings("UnusedDeclaration")
@TargetApi(8)
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
    public void setPlaybackState(State state) {
    }

}
