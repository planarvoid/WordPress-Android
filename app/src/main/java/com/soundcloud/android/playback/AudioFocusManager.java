package com.soundcloud.android.playback;

import android.content.Context;
import android.media.AudioManager;

public class AudioFocusManager implements IAudioManager {
    private boolean audioFocusLost;
    private AudioManager.OnAudioFocusChangeListener listener;

    private final Context context;

    public AudioFocusManager(final Context context) {
        this.context = context;
    }

    @Override
    public boolean requestMusicFocus(final MusicFocusable focusable, int durationHint) {
        if (listener == null) {
            listener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        if (audioFocusLost) {
                            focusable.focusGained();
                            audioFocusLost = false;
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        audioFocusLost = true;
                        focusable.focusLost(false, false);
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        audioFocusLost = true;
                        focusable.focusLost(true, false);
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        audioFocusLost = true;
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
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
}
