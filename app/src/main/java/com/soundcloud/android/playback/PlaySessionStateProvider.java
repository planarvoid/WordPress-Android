package com.soundcloud.android.playback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Player.StateTransition;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the current play session state. Can be queried for recent state, recent progress, and info about the current
 * track being played back.
 */

@Singleton
public class PlaySessionStateProvider {

    private final Map<Urn, PlaybackProgress> progressMap = new HashMap<>();
    private final Func1<StateTransition, Boolean> ignoreDefaultStateFilter = new Func1<StateTransition, Boolean>() {
        @Override
        public Boolean call(StateTransition stateTransition) {
            return !StateTransition.DEFAULT.equals(stateTransition);
        }
    };
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;

    private StateTransition lastStateTransition = StateTransition.DEFAULT;
    private Urn currentPlayingUrn; // the track that is currently loaded in the playback service

    @Inject
    public PlaySessionStateProvider(EventBus eventBus, PlayQueueManager playQueueManager) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM,  new PlayQueueTrackSubscriber());
        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .filter(PlayerFunctions.IS_NOT_VIDEO_AD)
                .filter(ignoreDefaultStateFilter)
                .subscribe(new PlayStateSubscriber());
    }

    public boolean isPlayingCurrentPlayQueueTrack(){
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        return currentPlayQueueItem.isTrack() &&
                isPlayingTrack(currentPlayQueueItem.getUrn());
    }

    @Deprecated
    public boolean isPlayingTrack(PublicApiTrack track) {
        return isPlayingTrack(track.getUrn());
    }

    public boolean isPlayingTrack(Urn trackUrn) {
        return currentPlayingUrn != null && currentPlayingUrn.equals(trackUrn);
    }

    public boolean isPlaying() {
        return lastStateTransition.playSessionIsActive();
    }

    public PlaybackProgress getLastProgressEvent() {
        return getLastProgressForTrack(currentPlayingUrn);
    }

    public PlaybackProgress getLastProgressEventForCurrentPlayQueueTrack() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        return currentPlayQueueItem.isTrack() ?
                getLastProgressForTrack(currentPlayQueueItem.getUrn()) : PlaybackProgress.empty();
    }

    public PlaybackProgress getLastProgressForTrack(Urn urn) {
        if (hasLastKnownProgress(urn)){
            return progressMap.get(urn);

        } else if (playQueueManager.wasLastSavedTrack(urn)) {
            return new PlaybackProgress(playQueueManager.getLastSavedProgressPosition(), Consts.NOT_SET);

        } else {
            return PlaybackProgress.empty();
        }
    }

    public boolean hasLastKnownProgress(Urn trackUrn) {
        return progressMap.containsKey(trackUrn);
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            final boolean isTrackChange = currentPlayingUrn != null &&
                    !stateTransition.isForUrn(currentPlayingUrn);

            if (isTrackChange && stateTransition.playSessionIsActive()) {
                progressMap.clear();
            }

            lastStateTransition = stateTransition;
            currentPlayingUrn = stateTransition.getUrn();

            if (stateTransition.getProgress().isDurationValid()) {
                progressMap.put(currentPlayingUrn, stateTransition.getProgress());
            }

            if (playingNewTrackFromBeginning(stateTransition, isTrackChange) || playbackStoppedMidSession(stateTransition)) {
                final long lastValidProgress = getLastProgressForTrack(currentPlayingUrn).getPosition();
                playQueueManager.saveCurrentProgress(stateTransition.trackEnded() ? 0 : lastValidProgress);
            }
        }
    }

    private boolean playbackStoppedMidSession(StateTransition stateTransition) {
        return (stateTransition.isPlayerIdle() && !stateTransition.isPlayQueueComplete());
    }

    private boolean playingNewTrackFromBeginning(StateTransition stateTransition, boolean isTrackChange) {
        return isTrackChange && !playQueueManager.wasLastSavedTrack(stateTransition.getUrn());
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        @Override
        public void onNext(PlaybackProgressEvent progress) {
            if (!progress.getUrn().isAd()) {
                progressMap.put(progress.getUrn(), progress.getPlaybackProgress());
            }
        }
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            if (lastStateTransition.playSessionIsActive()) {
                progressMap.clear();
            }
        }
    }
}
