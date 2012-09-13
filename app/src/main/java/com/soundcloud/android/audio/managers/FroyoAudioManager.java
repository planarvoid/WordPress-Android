package com.soundcloud.android.audio.managers;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;

@SuppressWarnings("UnusedDeclaration")
@TargetApi(8)
public class FroyoAudioManager implements IAudioManager {
    private boolean mAudioFocusLost;
    private AudioManager.OnAudioFocusChangeListener listener;

    private final Context mContext;

    public FroyoAudioManager(final Context context) {
        mContext = context;
    }

    @Override
    public boolean requestMusicFocus(final MusicFocusable focusable, int durationHint) {
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
                durationHint);

        if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            onFocusObtained();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean abandonMusicFocus(boolean isTemporary) {
        if (listener != null) {
            final int ret = getAudioManager().abandonAudioFocus(listener);
            if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                onFocusAbandoned();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onFocusObtained() {
    }

    @Override
    public void onFocusAbandoned() {
    }

    @Override
    public boolean isFocusSupported() {
        return true;
    }

    protected AudioManager getAudioManager() {
        return (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }


}
