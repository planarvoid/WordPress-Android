package com.soundcloud.android.playback.service;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class PlaybackStateProvider {

    @Inject
    public PlaybackStateProvider() {}

    @Nullable
    public PublicApiTrack getCurrentTrack() {
        final PlaybackService instance = PlaybackService.instance;
        return instance == null ? null : instance.getCurrentTrack();
    }

    public long getCurrentTrackId() {
        final PlaybackService instance = PlaybackService.instance;
        if (instance == null ){
            return PublicApiTrack.NOT_SET;
        } else {
            final PublicApiTrack currentTrack = instance.getCurrentTrack();
            return currentTrack == null ? PublicApiTrack.NOT_SET : currentTrack.getId();
        }
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

    public boolean isPlayingTrack(@NotNull PublicApiTrack track) {
        return getCurrentTrackId() == track.getId();
    }

    public boolean isSupposedToBePlaying() {
        final PlaybackService instance = PlaybackService.instance;
        return instance != null && instance.isPlaying();
    }
}
