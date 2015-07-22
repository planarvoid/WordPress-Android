package com.soundcloud.android.playback;

import com.soundcloud.java.collections.PropertySet;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;

@SuppressWarnings("UnusedDeclaration")
public class FallbackRemoteAudioManager extends AudioFocusManager implements IRemoteAudioManager {

    protected final Class<? extends BroadcastReceiver> RECEIVER = RemoteControlReceiver.class;
    protected final ComponentName receiverComponent;

    public FallbackRemoteAudioManager(final Context context) {
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
    public void onTrackChanged(PropertySet track, Bitmap artwork) {
    }

    @Override
    public void setPlaybackState(boolean isSupposedToBePlaying) {
    }

}
