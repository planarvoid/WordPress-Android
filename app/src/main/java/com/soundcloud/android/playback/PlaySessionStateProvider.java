package com.soundcloud.android.playback;

import com.google.common.collect.Maps;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa.StateTransition;
import com.soundcloud.android.playback.service.PlaybackProgressInfo;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Stores the current play session state. Can be queried for recent state, recent progress, and info about the current
 * track being played back.
 */

@Singleton
public class PlaySessionStateProvider {

    private final Map<TrackUrn, PlaybackProgress> progressMap = Maps.newHashMap();
    private final Func1<StateTransition, Boolean> ignoreDefaultStateFilter = new Func1<StateTransition, Boolean>() {
        @Override
        public Boolean call(StateTransition stateTransition) {
            return !StateTransition.DEFAULT.equals(stateTransition);
        }
    };
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;

    private StateTransition lastStateTransition = StateTransition.DEFAULT;
    private TrackUrn currentPlayingUrn; // the track that is currently loaded in the playback service

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

    public boolean isPlayingTrack(TrackUrn trackUrn) {
        return currentPlayingUrn != null && currentPlayingUrn.equals(trackUrn);
    }

    public boolean isPlaying() {
        return lastStateTransition.playSessionIsActive();
    }

    public PlaybackProgress getLastProgressEvent() {
        return getLastProgressByUrn(currentPlayingUrn);
    }

    public PlaybackProgress getCurrentPlayQueueTrackProgress() {
        final PlaybackProgress playbackProgress = progressMap.get(playQueueManager.getCurrentTrackUrn());
        return playbackProgress == null ? PlaybackProgress.empty() : playbackProgress;
    }

    public PlaybackProgress getLastProgressByUrn(TrackUrn trackUrn) {
        final PlaybackProgress playbackProgress = progressMap.get(trackUrn);
        return playbackProgress == null ? PlaybackProgress.empty() : playbackProgress;
    }

    public boolean hasCurrentProgress(TrackUrn trackUrn) {
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
                saveProgress(stateTransition);
            }
        }
    }

    private boolean playbackStoppedMidSession(StateTransition stateTransition) {
        return (stateTransition.isPlayerIdle() && !stateTransition.isPlayQueueComplete());
    }

    private boolean playingNewTrackFromBeginning(StateTransition stateTransition, boolean isTrackChange) {
        PlaybackProgressInfo info = playQueueManager.getPlayProgressInfo();
        return isTrackChange && (info == null || !info.shouldResumeTrack(stateTransition.getTrackUrn()));
    }

    private void saveProgress(StateTransition stateTransition) {
        playQueueManager.saveCurrentProgress(stateTransition.trackEnded() ? 0 : stateTransition.getProgress().getPosition());
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
