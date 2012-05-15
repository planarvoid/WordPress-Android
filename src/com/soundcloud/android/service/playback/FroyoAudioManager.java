package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;

@SuppressWarnings("UnusedDeclaration")
@SuppressLint("NewApi")
public class FroyoAudioManager implements IAudioManager {
    private boolean mAudioFocusLost;
    private AudioManager.OnAudioFocusChangeListener listener;

    private final Context mContext;

    protected final Class<? extends BroadcastReceiver> RECEIVER = RemoteControlReceiver.class;
    protected final ComponentName receiverComponent;

    public FroyoAudioManager(final Context context) {
        mContext = context;
        receiverComponent = new ComponentName(context, RECEIVER);
    }

    @Override
    public int requestMusicFocus(final MusicFocusable focusable) {
        if (listener == null) {
            listener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        if (mAudioFocusLost) {
                            focusable.focusGained();
                            mAudioFocusLost = false;
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        mAudioFocusLost = true;
                        focusable.focusLost(false, false);
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        mAudioFocusLost = true;
                        focusable.focusLost(true, false);
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        mAudioFocusLost = true;
                        focusable.focusLost(true, true);
                    }
                }
            };
        }
        final int ret = getAudioManager().requestAudioFocus(listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            onFocusObtained();
        }
        return ret;
    }

    @Override
    public int abandonMusicFocus(boolean isTemporary) {
        if (listener != null) {
            final int ret = getAudioManager().abandonAudioFocus(listener);
            if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                onFocusAbandoned();
            }
            return ret;
        } else {
            return AudioManager.AUDIOFOCUS_REQUEST_FAILED;
        }
    }

    @Override
    public void onFocusObtained() {
        registerMediaButton();
    }

    @Override
    public void onFocusAbandoned() {
        unregisterMediaButton();
    }

    @Override
    public void onTrackChanged(Track track, Bitmap artwork) {
    }

    @Override
    public void setPlaybackState(State state) {
    }

    @Override
    public boolean isFocusSupported() {
        return true;
    }

    @Override
    public boolean isTrackChangeSupported() {
        return false;
    }

    protected AudioManager getAudioManager() {
        return (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    private void registerMediaButton() {
        getAudioManager().registerMediaButtonEventReceiver(receiverComponent);
    }

    private void unregisterMediaButton() {
        getAudioManager().unregisterMediaButtonEventReceiver(receiverComponent);
    }
}
