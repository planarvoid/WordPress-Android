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
    private PlayStateEvent lastStateEvent = PlayStateEvent.DEFAULT;
    private Urn currentPlayingUrn = Urn.NOT_SET; // the urn of the item that is currently loaded in the playback service

    @Inject
    public PlaySessionStateProvider(PlaySessionStateStorage playSessionStateStorage) {
        this.playSessionStateStorage = playSessionStateStorage;
    }

    void onPlayStateTransition(PlayStateEvent playStateEvent) {
        if (!PlayStateEvent.DEFAULT.equals(playStateEvent)) {
            final boolean isItemChange = !playSessionStateStorage.getLastPlayingItem().equals(playStateEvent.getPlayingItemUrn());
            currentPlayingUrn = playStateEvent.getPlayingItemUrn();
            lastStateEvent = playStateEvent;
            lastProgress = playStateEvent.getProgress();

            if (playingNewItemFromBeginning(playStateEvent, isItemChange) || playStateEvent.getTransition().isPlayerIdle()) {
                playSessionStateStorage.saveProgress(getPositionIfPlayingTrack());
            }

            if (isItemChange) {
                playSessionStateStorage.savePlayInfo(playStateEvent.getPlayingItemUrn(), playStateEvent.getPlayId());
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

    private boolean playingNewItemFromBeginning(PlayStateEvent playStateEvent, boolean isItemChange) {
        return isItemChange && !isLastPlayed(playStateEvent.getPlayingItemUrn());
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
        return lastStateEvent.getTransition().playSessionIsActive();
    }

    public boolean isInErrorState() {
        return lastStateEvent.getTransition().wasError();
    }

    public PlaybackProgress getLastProgressEvent() {
        return lastProgress;
    }
}
