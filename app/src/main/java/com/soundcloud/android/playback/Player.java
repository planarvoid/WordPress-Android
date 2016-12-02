package com.soundcloud.android.playback;

public interface Player {

    void preload(PreloadItem preloadItem);

    void play(PlaybackItem playbackItem);

    void resume(PlaybackItem playbackItem);

    void pause();

    long seek(long ms, boolean performSeek);

    long getProgress();

    float getVolume();

    void setVolume(float v);

    void stop();

    void stopForTrackTransition();

    void destroy();

    void setListener(PlayerListener playerListener);

    // MediaPlayer specific. We can drop these when we drop mediaplayer, as they will be constant booleans in skippy
    boolean isSeekable();

    interface PlayerListener {

        PlayerListener EMPTY = new PlayerListener() {
            @Override
            public void onPlaystateChanged(PlaybackStateTransition stateTransition) {
                // no-op
            }

            @Override
            public void onProgressEvent(long progress, long duration) {
                // no-op
            }
        };

        void onPlaystateChanged(PlaybackStateTransition stateTransition);

        void onProgressEvent(long progress, long duration);

    }
}
