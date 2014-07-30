package com.soundcloud.android.playback;

import com.google.common.collect.Maps;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class PlaySessionStateProvider {
    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private final Map<TrackUrn, PlaybackProgress> progressMap = Maps.newHashMap();
    private final Func1<Playa.StateTransition, Boolean> ignoreDefaultStateFilter = new Func1<Playa.StateTransition, Boolean>() {
        @Override
        public Boolean call(Playa.StateTransition stateTransition) {
            return !Playa.StateTransition.DEFAULT.equals(stateTransition);
        }
    };
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;

    private Playa.StateTransition lastStateTransition = Playa.StateTransition.DEFAULT;
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

    public boolean isPlayingTrack(PublicApiTrack track) {
        return isPlayingTrack(track.getUrn());
    }

    public boolean isPlayingTrack(TrackUrn trackUrn) {
        return currentPlayingUrn != null && currentPlayingUrn.equals(trackUrn);
    }

    public boolean isPlaying() {
        return lastStateTransition.playSessionIsActive();
    }

    public PlaybackProgress getCurrentProgress() {
        return getCurrentProgress(currentPlayingUrn);
    }

    public PlaybackProgress getCurrentProgress(TrackUrn trackUrn) {
        final PlaybackProgress playbackProgress = progressMap.get(trackUrn);
        return playbackProgress == null ? PlaybackProgress.empty() : playbackProgress;
    }

    public boolean hasCurrentProgress(TrackUrn trackUrn) {
        return progressMap.containsKey(trackUrn);
    }

    public boolean isProgressWithinTrackChangeThreshold() {
        return getCurrentProgress().getPosition() < PROGRESS_THRESHOLD_FOR_TRACK_CHANGE;
    }

    private class PlayStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        @Override
        public void onNext(Playa.StateTransition stateTransition) {
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

            if (isTrackChange || (stateTransition.isPlayerIdle() && !stateTransition.isPlayQueueComplete())) {
                saveProgress(stateTransition);
            }
        }
    }

    private void saveProgress(Playa.StateTransition stateTransition) {
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
