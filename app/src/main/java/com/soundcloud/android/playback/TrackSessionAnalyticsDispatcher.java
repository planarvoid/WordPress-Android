package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class TrackSessionAnalyticsDispatcher implements PlaybackAnalyticsDispatcher {

    static final long CHECKPOINT_INTERVAL = TimeUnit.SECONDS.toMillis(30);

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final PlayQueueManager playQueueManager;
    private final AppboyPlaySessionState appboyPlaySessionState;
    private final StopReasonProvider stopReasonProvider;
    private final UuidProvider uuidProvider;

    private Optional<PlaybackSessionEvent> lastPlaySessionEvent = Optional.absent();
    private Optional<TrackSourceInfo> currentTrackSourceInfo = Optional.absent();
    private ReplaySubject<PropertySet> trackObservable;

    @Inject
    public TrackSessionAnalyticsDispatcher(EventBus eventBus,
                                           TrackRepository trackRepository,
                                           PlayQueueManager playQueueManager,
                                           AppboyPlaySessionState appboyPlaySessionState,
                                           StopReasonProvider stopReasonProvider,
                                           UuidProvider uuidProvider) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.playQueueManager = playQueueManager;
        this.appboyPlaySessionState = appboyPlaySessionState;
        this.stopReasonProvider = stopReasonProvider;
        this.uuidProvider = uuidProvider;
    }

    @Override
    public void onProgressEvent(PlaybackProgressEvent progressEvent) {
        // No-op
    }

    @Override
    public void onPlayTransition(PlaybackStateTransition transition, boolean isNewItem) {
        loadTrackIfChanged(transition, isNewItem);
        publishPlayEvent(transition);

    }

    @Override
    public void onStopTransition(PlaybackStateTransition transition, boolean isNewItem) {
        loadTrackIfChanged(transition, isNewItem);
        publishStopEvent(transition, stopReasonProvider.fromTransition(transition));
    }

    @Override
    public void onSkipTransition(PlaybackStateTransition transition) {
        publishStopEvent(transition, PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    @Override
    public void onProgressCheckpoint(PlaybackStateTransition previousTransition,
                                     final PlaybackProgressEvent progressEvent) {
        trackObservable
                .filter(new Func1<PropertySet, Boolean>() {
                    @Override
                    public Boolean call(PropertySet trackPropertySet) {
                        return isForPlayingTrack(progressEvent);
                    }
                })
                .map(stateTransitionToCheckpointEvent(previousTransition, progressEvent))
                .subscribe(eventBus.queue(EventQueue.TRACKING));
    }

    private void loadTrackIfChanged(PlaybackStateTransition transition, boolean isNewItem) {
        if (isNewItem) {
            trackObservable = ReplaySubject.createWithSize(1);
            trackRepository.track(transition.getUrn()).subscribe(trackObservable);
        }
    }

    private void publishPlayEvent(final PlaybackStateTransition stateTransition) {
        currentTrackSourceInfo = Optional.fromNullable(playQueueManager.getCurrentTrackSourceInfo());
        if (currentTrackSourceInfo.isPresent() && lastEventIsNotPlay()) {
            trackObservable
                    .map(stateTransitionToSessionPlayEvent(stateTransition))
                    .subscribe(eventBus.queue(EventQueue.TRACKING));
        }
    }

    private boolean lastEventIsNotPlay() {
        return !(lastPlaySessionEvent.isPresent() && lastPlaySessionEvent.get().isPlayEvent());
    }

    private Func1<PropertySet, PlaybackSessionEvent> stateTransitionToSessionPlayEvent(final PlaybackStateTransition stateTransition) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                PlaybackSessionEvent playSessionEvent = PlaybackSessionEvent.forPlay(buildEventArgs(track,
                                                                                                    stateTransition));

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

    private void publishStopEvent(final PlaybackStateTransition stateTransition, final int stopReason) {
        // note that we only want to publish a stop event if we have a corresponding play event. This value
        // will be nulled out after it is used, and we will not publish another stop event until a play event
        // creates a new value for lastSessionEventData
        if (lastPlaySessionEvent.isPresent() && currentTrackSourceInfo.isPresent()) {
            trackObservable
                    .map(new Func1<PropertySet, PlaybackSessionEvent>() {
                        @Override
                        public PlaybackSessionEvent call(PropertySet track) {
                            return PlaybackSessionEvent.forStop(lastPlaySessionEvent.get(),
                                                                stopReason,
                                                                buildEventArgs(track, stateTransition));
                        }
                    })
                    .subscribe(eventBus.queue(EventQueue.TRACKING));
            lastPlaySessionEvent = Optional.absent();
        }
    }

    private Func1<PropertySet, TrackingEvent> stateTransitionToCheckpointEvent(final PlaybackStateTransition stateTransition,
                                                                               final PlaybackProgressEvent progressEvent) {
        return new Func1<PropertySet, TrackingEvent>() {
            @Override
            public TrackingEvent call(PropertySet track) {
                return PlaybackSessionEvent.forCheckpoint(buildEventArgs(track,
                                                                         progressEvent.getPlaybackProgress(),
                                                                         stateTransition));
            }
        };
    }

    private boolean isForPlayingTrack(PlaybackProgressEvent progressEvent) {
        return lastPlaySessionEvent.isPresent() && lastPlaySessionEvent.get()
                                                                       .getTrackUrn()
                                                                       .equals(progressEvent.getUrn());
    }

    @NonNull
    private PlaybackSessionEventArgs buildEventArgs(PropertySet track, PlaybackStateTransition stateTransition) {
        return buildEventArgs(track, stateTransition.getProgress(), stateTransition);
    }

    private PlaybackSessionEventArgs buildEventArgs(PropertySet track,
                                                    PlaybackProgress progress,
                                                    PlaybackStateTransition stateTransition) {
        return PlaybackSessionEventArgs.createWithProgress(track,
                                                           currentTrackSourceInfo.get(),
                                                           progress,
                                                           stateTransition,
                                                           appboyPlaySessionState.isMarketablePlay(),
                                                           uuidProvider.getRandomUuid());
    }

}
