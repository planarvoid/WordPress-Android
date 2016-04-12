package com.soundcloud.android.playback;

public interface Player {

    void play(PlaybackItem playbackItem);

    void resume();

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

        void onPlaystateChanged(PlaybackStateTransition stateTransition);

        void onProgressEvent(long progress, long duration);

        // we might be able to get rid of this, if we just request focus before setting data source, however this is a change in behavior
        boolean requestAudioFocus();
    }
}
