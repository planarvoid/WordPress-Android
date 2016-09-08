package com.soundcloud.android.playback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.UuidProvider;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Stores the current play session state. Can be queried for recent state, recent progress, and info about the current
 * item being played back.
 */

@Singleton
public class PlaySessionStateProvider {

    private final PlaySessionStateStorage playSessionStateStorage;
    private final UuidProvider uuidProvider;

    private PlaybackProgress lastProgress = PlaybackProgress.empty();
    private PlayStateEvent lastStateEvent = PlayStateEvent.DEFAULT;
    private Urn currentPlayingUrn = Urn.NOT_SET; // the urn of the item that is currently loaded in the playback service

    @Inject
    public PlaySessionStateProvider(PlaySessionStateStorage playSessionStateStorage, UuidProvider uuidProvider) {
        this.playSessionStateStorage = playSessionStateStorage;
        this.uuidProvider = uuidProvider;
    }

    public PlayStateEvent onPlayStateTransition(PlaybackStateTransition stateTransition, long duration) {
        if (!PlaybackStateTransition.DEFAULT.equals(stateTransition)) {

            final boolean isNewTrack = !isLastPlayed(stateTransition.getUrn());
            if (isNewTrack) {
                playSessionStateStorage.savePlayInfo(stateTransition.getUrn());
            }

            PlayStateEvent playStateEvent = getPlayStateEvent(stateTransition, duration);
            if (playStateEvent.isFirstPlay()) {
                playSessionStateStorage.savePlayId(playStateEvent.getPlayId());
            }

            currentPlayingUrn = playStateEvent.isTrackComplete() ? Urn.NOT_SET : stateTransition.getUrn();
            lastStateEvent = playStateEvent;
            lastProgress = playStateEvent.getProgress();

            if (isNewTrack || playStateEvent.getTransition().isPlayerIdle()) {
                playSessionStateStorage.saveProgress(getPositionOfPlayingTrack());
            }
            return playStateEvent;

        }
        return PlayStateEvent.DEFAULT;
    }

    @NonNull
    public PlayStateEvent getPlayStateEvent(PlaybackStateTransition stateTransition, long duration) {
        final boolean isFirstPlay = stateTransition.isPlayerPlaying() && !playSessionStateStorage.hasPlayId();
        final String playId = isFirstPlay ? uuidProvider.getRandomUuid() : playSessionStateStorage.getLastPlayId();
        return PlayStateEvent.create(stateTransition, duration, isFirstPlay, playId);
    }

    private long getPositionOfPlayingTrack() {
        return currentPlayingUrn.isTrack() ? getLastProgressForItem(currentPlayingUrn).getPosition() : 0;
    }

    void onProgressEvent(PlaybackProgressEvent progress) {
        if (progress.getUrn().equals(currentPlayingUrn)) {
            lastProgress = progress.getPlaybackProgress();
        }
    }

    public PlaybackProgress getLastProgressForItem(Urn urn) {
        if (currentPlayingUrn.equals(urn) && lastProgress.isDurationValid()) {
            return lastProgress;
        } else if (isLastPlayed(urn)) {
            return new PlaybackProgress(getLastSavedProgressPosition(), Consts.NOT_SET);
        } else {
            return PlaybackProgress.empty();
        }
    }

    void clearLastProgressForItem(Urn urn) {
        if (currentPlayingUrn.equals(urn) || isLastPlayed(urn)) {
            playSessionStateStorage.saveProgress(0);
            lastProgress = PlaybackProgress.empty();
        }
    }

    public boolean isLastPlayed(Urn urn) {
        return playSessionStateStorage.getLastPlayingItem().equals(urn);
    }

    public boolean isCurrentlyPlaying(Urn urn) {
        return currentPlayingUrn.equals(urn);
    }


    public long getLastSavedProgressPosition() {
        return playSessionStateStorage.getLastStoredProgress();
    }

    public String getCurrentPlayId() {
        return playSessionStateStorage.getLastPlayId();
    }

    public boolean isPlaying() {
        return lastStateEvent.playSessionIsActive();
    }

    public boolean isInErrorState() {
        return lastStateEvent.getTransition().wasError();
    }

    public PlaybackProgress getLastProgressEvent() {
        return lastProgress;
    }
}
