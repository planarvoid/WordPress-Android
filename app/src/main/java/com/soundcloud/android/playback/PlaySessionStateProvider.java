package com.soundcloud.android.playback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Playa.StateTransition;
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
        eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK,  new PlayQueueTrackSubscriber());
        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED).filter(ignoreDefaultStateFilter)
                .subscribe(new PlayStateSubscriber());
    }

    public boolean isPlayingCurrentPlayQueueTrack(){
        return isPlayingTrack(playQueueManager.getCurrentTrackUrn());
    }

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
        return getLastProgressForTrack(playQueueManager.getCurrentTrackUrn());
    }

    public PlaybackProgress getLastProgressForTrack(Urn urn) {
        if (hasLastKnownProgress(urn)){
            return progressMap.get(urn);

        } else if (playQueueManager.wasLastSavedTrack(urn)) {
            return new PlaybackProgress(playQueueManager.getLastSavedPosition(), Consts.NOT_SET);

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
                    !stateTransition.isForTrack(currentPlayingUrn);

            if (isTrackChange && stateTransition.playSessionIsActive()) {
                progressMap.clear();
            }

            lastStateTransition = stateTransition;
            currentPlayingUrn = stateTransition.getTrackUrn();

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
        return isTrackChange && !playQueueManager.wasLastSavedTrack(stateTransition.getTrackUrn());
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        @Override
        public void onNext(PlaybackProgressEvent progress) {
            progressMap.put(progress.getTrackUrn(), progress.getPlaybackProgress());
        }
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            if (lastStateTransition.playSessionIsActive()) {
                progressMap.clear();
            }
        }
    }
}
