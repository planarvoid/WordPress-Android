package com.soundcloud.android.playback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Stores the current play session state. Can be queried for recent state, recent progress, and info about the current
 * item being played back.
 */

@Singleton
public class PlaySessionStateProvider {

    private final PlaySessionStateStorage playSessionStateStorage;

    private PlaybackProgress lastProgress = PlaybackProgress.empty();
    private PlaybackStateTransition lastStateTransition = PlaybackStateTransition.DEFAULT;
    private Urn currentPlayingUrn = Urn.NOT_SET; // the urn of the item that is currently loaded in the playback service

    @Inject
    public PlaySessionStateProvider(PlaySessionStateStorage playSessionStateStorage) {
        this.playSessionStateStorage = playSessionStateStorage;
    }

    void onPlayStateTransition(PlaybackStateTransition stateTransition) {
        if (!PlaybackStateTransition.DEFAULT.equals(stateTransition)) {
            final boolean isItemChange = !playSessionStateStorage.getLastPlayingItem().equals(stateTransition.getUrn());
            currentPlayingUrn = stateTransition.getUrn();
            lastStateTransition = stateTransition;
            lastProgress = stateTransition.getProgress();

            if (playingNewItemFromBeginning(stateTransition,
                                            isItemChange) || playbackStoppedMidSession(stateTransition)) {
                playSessionStateStorage.saveProgress(getPositionIfPlayingTrack());
            }

            if (isItemChange) {
                playSessionStateStorage.savePlayInfo(stateTransition.getUrn());
            }
        }
    }

    private long getPositionIfPlayingTrack() {
        return currentPlayingUrn.isTrack() ? getLastProgressForItem(currentPlayingUrn).getPosition() : Consts.NOT_SET;
    }

    void onProgressEvent(PlaybackProgressEvent progress) {
        if (progress.getUrn().equals(currentPlayingUrn)) {
            lastProgress = progress.getPlaybackProgress();
        }
    }

    private boolean playbackStoppedMidSession(PlaybackStateTransition stateTransition) {
        return (stateTransition.isPlayerIdle() && !stateTransition.isPlayQueueComplete());
    }

    private boolean playingNewItemFromBeginning(PlaybackStateTransition stateTransition, boolean isItemChange) {
        return isItemChange && !isCurrentlyPlaying(stateTransition.getUrn());
    }

    public PlaybackProgress getLastProgressForItem(Urn urn) {
        if (currentPlayingUrn.equals(urn) && lastProgress.isDurationValid()) {
            return lastProgress;

        } else if (isCurrentlyPlaying(urn)) {
            return new PlaybackProgress(getLastSavedProgressPosition(), Consts.NOT_SET);

        } else {
            return PlaybackProgress.empty();
        }
    }

    public boolean isCurrentlyPlaying(Urn urn) {
        return playSessionStateStorage.getLastPlayingItem().equals(urn);
    }

    public long getLastSavedProgressPosition() {
        return playSessionStateStorage.getLastStoredProgress();
    }

    public boolean isPlaying() {
        return lastStateTransition.playSessionIsActive();
    }

    public boolean isInErrorState() {
        return lastStateTransition.wasError();
    }

    public PlaybackProgress getLastProgressEvent() {
        return lastProgress;
    }
}
