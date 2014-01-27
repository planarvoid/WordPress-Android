package com.soundcloud.android.playback.service;

import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class PlaybackStateProvider {

    @Inject
    public PlaybackStateProvider() {
    }

    @Nullable
    public Track getCurrentTrack() {
        final PlaybackService instance = PlaybackService.instance;
        return instance == null ? null : instance.getCurrentTrack();
    }

    public long getCurrentTrackId() {
        final PlayQueueView playQueue = getPlayQueue();
        return PlaybackService.instance == null || playQueue.isEmpty() ? ScModel.NOT_SET : playQueue.getCurrentTrackId();
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
        final PlaybackService instance = PlaybackService.instance;
        return instance == null ? -1 : instance.loadPercent();
    }

    public boolean isBuffering() {
        final PlaybackService instance = PlaybackService.instance;
        return instance != null && instance._isBuffering();
    }

    public boolean isSeekable() {
        final PlaybackService instance = PlaybackService.instance;
        return instance != null && instance._isSeekable();
    }

    public boolean isPlaying() {
        final PlaybackService instance = PlaybackService.instance;
        return getPlaybackState(instance).equals(PlaybackState.PLAYING);
    }

    public boolean isSupposedToBePlaying() {
        final PlaybackService instance = PlaybackService.instance;
        return getPlaybackState(instance).isSupposedToBePlaying();
    }

    private PlaybackState getPlaybackState(@Nullable PlaybackService instance) {
        return (instance == null ? PlaybackState.STOPPED : instance.getPlaybackStateInternal());
    }
}
