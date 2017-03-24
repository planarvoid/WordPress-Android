package com.soundcloud.android.playback;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.subjects.BehaviorSubject;

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
    private final PlaybackProgressRepository playbackProgressRepository;

    private PlayStateEvent lastStateEvent = PlayStateEvent.DEFAULT;
    private Urn currentPlayingUrn = Urn.NOT_SET; // the urn of the item that is currently loaded in the playback service
    private BehaviorSubject<Urn> nowPlayingUrn = BehaviorSubject.create(currentPlayingUrn);
    private long lastStateEventTime;

    @Inject
    public PlaySessionStateProvider(PlaySessionStateStorage playSessionStateStorage,
                                    UuidProvider uuidProvider,
                                    EventBus eventBus,
                                    CurrentDateProvider dateProvider,
                                    PlaybackProgressRepository playbackProgressRepository) {
        this.playSessionStateStorage = playSessionStateStorage;
        this.uuidProvider = uuidProvider;
        this.eventBus = eventBus;
        this.dateProvider = dateProvider;
        this.playbackProgressRepository = playbackProgressRepository;
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

        setCurrentPlayingUrn(playStateEvent.isTrackComplete() ? Urn.NOT_SET : stateTransition.getUrn());
        lastStateEvent = playStateEvent;
        lastStateEventTime = dateProvider.getCurrentTime();
        playbackProgressRepository.put(currentPlayingUrn, playStateEvent.getProgress());

        if (isNewTrack || playStateEvent.getTransition().isPlayerIdle()) {
            if (currentPlayingUrn.isTrack()) {
                PlaybackProgress lastProgressForCurrentlyPlayingTrack = getLastProgressForItem(currentPlayingUrn);
                playSessionStateStorage.saveProgress(lastProgressForCurrentlyPlayingTrack.getPosition(), lastProgressForCurrentlyPlayingTrack.getDuration());
            } else {
                playSessionStateStorage.clearProgressAndDuration();
            }
        }
        return playStateEvent;
    }

    @NonNull
    private PlayStateEvent getPlayStateEvent(PlaybackStateTransition stateTransition, long duration) {
        final boolean isFirstPlay = stateTransition.isPlayerPlaying() && !playSessionStateStorage.hasPlayId();
        final String playId = isFirstPlay ? uuidProvider.getRandomUuid() : playSessionStateStorage.getLastPlayId();
        return PlayStateEvent.create(stateTransition, duration, isFirstPlay, playId);
    }

    public void onProgressEvent(PlaybackProgressEvent progress) {
        playbackProgressRepository.put(progress.getUrn(), progress.getPlaybackProgress());
        if (isCurrentlyPlaying(progress.getUrn())) {
            eventBus.publish(EventQueue.PLAYBACK_PROGRESS, progress);
        }
    }

    public PlaybackProgress getLastProgressForItem(Urn urn) {
        if (!isCurrentlyPlaying(urn) && isLastPlayed(urn)) {
            long lastPosition = playSessionStateStorage.getLastStoredProgress();
            long lastDuration = playSessionStateStorage.getLastStoredDuration();
            return new PlaybackProgress(lastPosition, lastDuration, urn);
        } else {
            return getProgressForUrn(urn);
        }
    }

    void clearLastProgressForItem(Urn urn) {
        playbackProgressRepository.remove(urn);
        if (isCurrentlyPlaying(urn) || isLastPlayed(urn)) {
            playSessionStateStorage.clearProgressAndDuration();
        }
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, PlaybackProgressEvent.create(PlaybackProgress.empty(), urn));
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
        return getProgressForUrn(currentPlayingUrn);
    }

    private PlaybackProgress getProgressForUrn(Urn urn) {
        return playbackProgressRepository.get(urn).or(PlaybackProgress.empty());
    }

    public long getMillisSinceLastPlaySession() {
        return isPlaying() ? 0 : (dateProvider.getCurrentTime() - lastStateEventTime);
    }

    public Observable<Urn> nowPlayingUrn() {
        return nowPlayingUrn;
    }

    private void setCurrentPlayingUrn(Urn urn) {
        this.currentPlayingUrn = urn;
        nowPlayingUrn.onNext(urn);
    }
}
