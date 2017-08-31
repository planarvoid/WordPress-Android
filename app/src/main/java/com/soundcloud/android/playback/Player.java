package com.soundcloud.android.playback;

import com.soundcloud.android.events.PlayerType;
import org.jetbrains.annotations.NotNull;

public interface Player {

    void preload(@NotNull PreloadItem preloadItem);

    void play(@NotNull PlaybackItem playbackItem);

    void resume(@NotNull PlaybackItem playbackItem);

    void pause();

    void seek(long ms);

    long getProgress();

    float getVolume();

    void setVolume(float v);

    void stop();

    void stopForTrackTransition();

    void destroy();

    void setListener(@NotNull PlayerListener playerListener);

    // MediaPlayer specific. We can drop these when we drop mediaplayer, as they will be constant booleans in skippy
    boolean isSeekable();

    PlayerType getPlayerType();

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
