package com.soundcloud.android.playback.service;

import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class PlaybackStateProvider implements OriginProvider {

    @Inject
    public PlaybackStateProvider() {}

    @Nullable
    public Track getCurrentTrack() {
        final PlaybackService instance = PlaybackService.instance;
        return instance == null ? null : instance.getCurrentTrack();
    }

    public long getCurrentTrackId() {
        final PlayQueueView playQueue = getPlayQueue();
        return PlaybackService.instance == null || playQueue == null || playQueue.isEmpty()
                ? ScModel.NOT_SET : playQueue.getCurrentTrackId();
    }

    public PlayQueueView getPlayQueue() {
        final PlaybackService instance = PlaybackService.instance;
        return instance == null ? PlayQueueView.EMPTY : instance.getPlayQueueView();
    }

    public int getPlayPosition() {
        final PlaybackService instance = PlaybackService.instance;
        return instance == null ? -1 : instance.getPlayQueueInternal().getPosition();
    }

    public long getPlayProgress() {
        final PlaybackService instance = PlaybackService.instance;
        return instance == null ? -1 : instance.getProgress();
    }

    public int getLoadingPercent() {
        //Need to still implement this for skippy but add it to the media player adapter
        return -1;
    }

    public boolean isBuffering() {
        final PlaybackService instance = PlaybackService.instance;
        return instance != null && instance.isBuffering();
    }

    public boolean isSeekable() {
        final PlaybackService instance = PlaybackService.instance;
        return instance != null && instance.isSeekable();
    }

    public boolean isPlaying() {
        final PlaybackService instance = PlaybackService.instance;
        return instance != null && instance.isPlayerPlaying();
    }

    public boolean isSupposedToBePlaying() {
        final PlaybackService instance = PlaybackService.instance;
        return instance != null && instance.isPlaying();
    }

    public long getPlayQueuePlaylistId() {
        final PlaybackService instance = PlaybackService.instance;
        return instance == null ? Playable.NOT_SET : instance.getPlayQueuePlaylistId();
    }

    public boolean isPlaylistPlaying(long playlistId) {
        return getPlayQueuePlaylistId() == playlistId && isSupposedToBePlaying();
    }

    @Override
    public String getScreenTag() {
        final PlaybackService instance = PlaybackService.instance;
        return instance == null ? Screen.UNKNOWN.get() : instance.getPlayQueueOriginScreen();
    }
}
