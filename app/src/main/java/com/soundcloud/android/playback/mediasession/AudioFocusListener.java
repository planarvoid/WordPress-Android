package com.soundcloud.android.playback.mediasession;

import android.media.AudioManager;

class AudioFocusListener implements AudioManager.OnAudioFocusChangeListener {

    private final MediaSessionController.Listener listener;

    AudioFocusListener(MediaSessionController.Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                listener.onFocusLoss(false, false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                listener.onFocusLoss(true, false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                listener.onFocusLoss(true, true);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                listener.onFocusGain();
                break;
            default:
                break;
        }
    }
}
