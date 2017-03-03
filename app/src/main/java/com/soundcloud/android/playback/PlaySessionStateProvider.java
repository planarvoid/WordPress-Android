package com.soundcloud.android.playback;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.rx.eventbus.EventBus;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

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
    private final EventBus eventBus;
    private final CurrentDateProvider dateProvider;

    private PlaybackProgress lastProgress = PlaybackProgress.empty();
    private PlayStateEvent lastStateEvent = PlayStateEvent.DEFAULT;
    private Urn currentPlayingUrn = Urn.NOT_SET; // the urn of the item that is currently loaded in the playback service
    private long lastStateEventTime;

    @Inject
    public PlaySessionStateProvider(PlaySessionStateStorage playSessionStateStorage,
                                    UuidProvider uuidProvider,
                                    EventBus eventBus,
                                    CurrentDateProvider dateProvider) {
        this.playSessionStateStorage = playSessionStateStorage;
        this.uuidProvider = uuidProvider;
        this.eventBus = eventBus;
        this.dateProvider = dateProvider;
    }

    public PlayStateEvent onPlayStateTransition(PlaybackStateTransition stateTransition, long duration) {
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
        lastStateEventTime = dateProvider.getCurrentTime();
        lastProgress = playStateEvent.getProgress();

        if (isNewTrack || playStateEvent.getTransition().isPlayerIdle()) {
            playSessionStateStorage.saveProgress(getPositionOfPlayingTrack(), getDurationOfPlayingTrack());
        }
        return playStateEvent;
    }

    @NonNull
    private PlayStateEvent getPlayStateEvent(PlaybackStateTransition stateTransition, long duration) {
        final boolean isFirstPlay = stateTransition.isPlayerPlaying() && !playSessionStateStorage.hasPlayId();
        final String playId = isFirstPlay ? uuidProvider.getRandomUuid() : playSessionStateStorage.getLastPlayId();
        return PlayStateEvent.create(stateTransition, duration, isFirstPlay, playId);
    }

    private long getPositionOfPlayingTrack() {
        return currentPlayingUrn.isTrack() ? getLastProgressForItem(currentPlayingUrn).getPosition() : 0;
    }

    private long getDurationOfPlayingTrack() {
        return currentPlayingUrn.isTrack() ? getLastProgressForItem(currentPlayingUrn).getDuration() : 0;
    }

    public void onProgressEvent(PlaybackProgressEvent progress) {
        if (progress.getUrn().equals(currentPlayingUrn)) {
            lastProgress = progress.getPlaybackProgress();
            eventBus.publish(EventQueue.PLAYBACK_PROGRESS, progress);
        }
    }

    public PlaybackProgress getLastProgressForItem(Urn urn) {
        if (currentPlayingUrn.equals(urn) && lastProgress.isDurationValid()) {
            return lastProgress;
        } else if (isLastPlayed(urn)) {
            long lastPosition = playSessionStateStorage.getLastStoredProgress();
            long lastDuration = playSessionStateStorage.getLastStoredDuration();
            return new PlaybackProgress(lastPosition, lastDuration, urn);
        } else {
            return PlaybackProgress.empty();
        }
    }

    void clearLastProgressForItem(Urn urn) {
        if (currentPlayingUrn.equals(urn) || isLastPlayed(urn)) {
            lastProgress = PlaybackProgress.empty();
            playSessionStateStorage.saveProgress(lastProgress.getPosition(), lastProgress.getDuration());
        }
    }

    @VisibleForTesting
    boolean isLastPlayed(Urn urn) {
        return playSessionStateStorage.getLastPlayingItem().equals(urn);
    }

    public boolean isCurrentlyPlaying(Urn urn) {
        return currentPlayingUrn.equals(urn);
    }

    public boolean wasLastStateACastDisconnection() {
        return lastStateEvent.isCastDisconnection();
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

    public long getMillisSinceLastPlaySession() {
        return isPlaying() ? 0 : (dateProvider.getCurrentTime() - lastStateEventTime);
    }

}
