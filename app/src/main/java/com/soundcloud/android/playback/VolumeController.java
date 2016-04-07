package com.soundcloud.android.playback;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;

@AutoFactory(allowSubclasses = true)
class VolumeController implements FadeHandler.Listener {

    interface Listener {
        void onFadeFinished();
    }

    private static final float DUCK_VOLUME = 0.1f;
    private static final float MAX_VOLUME = 1.0f;
    private static final float MIN_VOLUME = 0.0f;

    private final FadeHandler fadeHandler;

    private boolean ducked = false;
    private boolean muted = false;
    private boolean fading_in = false;
    private boolean fading_out = false;

    private final StreamPlayer streamPlayer;
    private final Listener listener;

    VolumeController(StreamPlayer streamPlayer,
                     Listener listener,
                     @Provided FadeHandlerFactory fadeHandlerFactory) {
        this.streamPlayer = streamPlayer;
        this.listener = listener;
        this.fadeHandler = fadeHandlerFactory.create(this);
    }

    void mute(long duration) {
        if (!muted) {
            muted = true;
            fadeOut(duration, 0);
        }
    }

    void unMute(long duration) {
        if (muted || ducked) {
            muted = false;
            ducked = false;
            fadeIn(duration, 0);
        }
    }

    void duck(long duration) {
        if (!ducked && !muted) {
            ducked = true;
            fadeOut(duration, 0);
        }
    }

    void resetVolume() {
        if (!ducked && !muted) {
            fadeHandler.stop();
            setVolume(MAX_VOLUME);
            fading_in = false;
            fading_out = false;
        }
    }

    void fadeIn(long duration, long offset) {
        if (!fading_in && !muted) {
            fading_in = true;
            fading_out = false;
            fade(duration, offset, ducked ? DUCK_VOLUME : MAX_VOLUME);
        }
    }

    void fadeOut(long duration, long offset) {
        if (!fading_out) {
            fading_out = true;
            fading_in = false;
            fade(duration, offset, ducked ? DUCK_VOLUME : MIN_VOLUME);
        }
    }

    private void fade(long duration, long offset, float target) {
        FadeRequest fadeRequest = FadeRequest
                .builder()
                .duration(duration)
                .offset(offset)
                .startValue(streamPlayer.getVolume())
                .endValue(target)
                .build();

        fadeHandler.fade(fadeRequest);
    }

    private void setVolume(float volume) {
        streamPlayer.setVolume(volume);
    }

    @Override
    public void onFade(float volume) {
        if (fading_in || fading_out) {
            setVolume(volume);
        }
    }

    @Override
    public void onFadeFinished() {
        if (listener != null) {
            listener.onFadeFinished();
        }
    }

}
