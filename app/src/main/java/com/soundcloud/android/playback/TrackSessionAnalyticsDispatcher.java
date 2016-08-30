package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
class TrackSessionAnalyticsDispatcher implements PlaybackAnalyticsDispatcher {

    static final long CHECKPOINT_INTERVAL = TimeUnit.SECONDS.toMillis(30);

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final PlayQueueManager playQueueManager;
    private final AppboyPlaySessionState appboyPlaySessionState;
    private final StopReasonProvider stopReasonProvider;

    private Optional<PlaybackSessionEvent> lastPlaySessionEvent = Optional.absent();
    private Optional<TrackSourceInfo> currentTrackSourceInfo = Optional.absent();
    private ReplaySubject<PropertySet> trackObservable;

    @Inject
    public TrackSessionAnalyticsDispatcher(EventBus eventBus,
                                           TrackRepository trackRepository,
                                           PlayQueueManager playQueueManager,
                                           AppboyPlaySessionState appboyPlaySessionState,
                                           StopReasonProvider stopReasonProvider) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.playQueueManager = playQueueManager;
        this.appboyPlaySessionState = appboyPlaySessionState;
        this.stopReasonProvider = stopReasonProvider;
    }

    @Override
    public void onProgressEvent(PlaybackProgressEvent progressEvent) {
        // No-op
    }

    @Override
    public void onPlayTransition(PlayStateEvent playStateEvent, boolean isNewItem) {
        loadTrackIfChanged(playStateEvent, isNewItem);
        publishPlayEvent(playStateEvent);

    }

    @Override
    public void onStopTransition(PlayStateEvent playStateEvent, boolean isNewItem) {
        loadTrackIfChanged(playStateEvent, isNewItem);
        publishStopEvent(playStateEvent, stopReasonProvider.fromTransition(playStateEvent.getTransition()));
    }

    @Override
    public void onSkipTransition(PlayStateEvent playStateEvent) {
        publishStopEvent(playStateEvent, PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    @Override
    public void onProgressCheckpoint(PlayStateEvent previousPlayStateEvent,
                                     final PlaybackProgressEvent progressEvent) {
        trackObservable
                .filter(new Func1<PropertySet, Boolean>() {
                    @Override
                    public Boolean call(PropertySet trackPropertySet) {
                        return isForPlayingTrack(progressEvent);
                    }
                })
                .map(stateTransitionToCheckpointEvent(previousPlayStateEvent, progressEvent))
                .subscribe(eventBus.queue(EventQueue.TRACKING));
    }

    private void loadTrackIfChanged(PlayStateEvent playStateEvent, boolean isNewItem) {
        if (isNewItem) {
            trackObservable = ReplaySubject.createWithSize(1);
            trackRepository.track(playStateEvent.getPlayingItemUrn()).subscribe(trackObservable);
        }
    }

    private void publishPlayEvent(final PlayStateEvent playStateEvent) {
        currentTrackSourceInfo = Optional.fromNullable(playQueueManager.getCurrentTrackSourceInfo());
        if (currentTrackSourceInfo.isPresent() && lastEventIsNotPlay()) {
            trackObservable
                    .map(playStateToSessionPlayEvent(playStateEvent))
                    .subscribe(eventBus.queue(EventQueue.TRACKING));
        }
    }

    private boolean lastEventIsNotPlay() {
        return !(lastPlaySessionEvent.isPresent() && lastPlaySessionEvent.get().isPlayEvent());
    }

    private Func1<PropertySet, PlaybackSessionEvent> playStateToSessionPlayEvent(final PlayStateEvent playStateEvent) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                PlaybackSessionEvent playSessionEvent = PlaybackSessionEvent.forPlay(buildEventArgs(track,
                                                                                                    playStateEvent));

                final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
                PlaySessionSource playSource = playQueueManager.getCurrentPlaySessionSource();

                if (currentPlayQueueItem.isTrack()
                        && playQueueManager.isTrackFromCurrentPromotedItem(currentPlayQueueItem.getUrn())
                        && !playSource.getPromotedSourceInfo().isPlaybackStarted()) {
                    PromotedSourceInfo promotedSourceInfo = playSource.getPromotedSourceInfo();
                    playSessionEvent = playSessionEvent.withPromotedTrack(promotedSourceInfo);
                    promotedSourceInfo.setPlaybackStarted();
                }

                lastPlaySessionEvent = Optional.of(playSessionEvent);
                return playSessionEvent;
            }
        };
    }

    private void publishStopEvent(final PlayStateEvent playStateEvent, final int stopReason) {
        // note that we only want to publish a stop event if we have a corresponding play event. This value
        // will be nulled out after it is used, and we will not publish another stop event until a play event
        // creates a new value for lastSessionEventData
        if (lastPlaySessionEvent.isPresent() && currentTrackSourceInfo.isPresent()) {
            final PlaybackSessionEvent playEventForStop = lastPlaySessionEvent.get();
            trackObservable
                    .map(new Func1<PropertySet, PlaybackSessionEvent>() {
                        @Override
                        public PlaybackSessionEvent call(PropertySet track) {
                            return PlaybackSessionEvent.forStop(playEventForStop,
                                                                stopReason,
                                                                buildEventArgs(track, playStateEvent));
                        }
                    })
                    .subscribe(eventBus.queue(EventQueue.TRACKING));
            lastPlaySessionEvent = Optional.absent();
        }
    }

    private Func1<PropertySet, TrackingEvent> stateTransitionToCheckpointEvent(final PlayStateEvent playStateEvent,
                                                                               final PlaybackProgressEvent progressEvent) {
        return new Func1<PropertySet, TrackingEvent>() {
            @Override
            public TrackingEvent call(PropertySet track) {
                return PlaybackSessionEvent.forCheckpoint(buildEventArgs(track,
                                                                         progressEvent.getPlaybackProgress(),
                                                                         playStateEvent));
            }
        };
    }

    private boolean isForPlayingTrack(PlaybackProgressEvent progressEvent) {
        return lastPlaySessionEvent.isPresent() && lastPlaySessionEvent.get()
                                                                       .getTrackUrn()
                                                                       .equals(progressEvent.getUrn());
    }

    @NonNull
    private PlaybackSessionEventArgs buildEventArgs(PropertySet track, PlayStateEvent playStateEvent) {
        return buildEventArgs(track, playStateEvent.getProgress(), playStateEvent);
    }

    private PlaybackSessionEventArgs buildEventArgs(PropertySet track,
                                                    PlaybackProgress progress,
                                                    PlayStateEvent playStateEvent) {
        return PlaybackSessionEventArgs.createWithProgress(track,
                                                           currentTrackSourceInfo.get(),
                                                           progress,
                                                           playStateEvent.getTransition(),
                                                           appboyPlaySessionState.isMarketablePlay(),
                                                           UUID.randomUUID().toString());
    }

}
