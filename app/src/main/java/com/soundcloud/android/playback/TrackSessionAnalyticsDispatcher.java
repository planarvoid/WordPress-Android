package com.soundcloud.android.playback;

import static com.soundcloud.android.ApplicationModule.CURRENT_DATE_PROVIDER;
import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

class TrackSessionAnalyticsDispatcher implements PlaybackAnalyticsDispatcher {

    private static final int CHECKPOINT_INTERVAL = 30;

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final AccountOperations accountOperations;
    private final PlayQueueManager playQueueManager;
    private final AppboyPlaySessionState appboyPlaySessionState;
    private final StopReasonProvider stopReasonProvider;
    private final UuidProvider uuidProvider;
    private final DateProvider dateProvider;
    private final Scheduler scheduler;

    private Optional<PlaybackSessionEvent> lastPlaySessionEvent = Optional.absent();
    private Optional<TrackSourceInfo> currentTrackSourceInfo = Optional.absent();
    private ReplaySubject<PropertySet> trackObservable;
    private Subscription checkpointSubscription = RxUtils.invalidSubscription();

    @Inject
    public TrackSessionAnalyticsDispatcher(EventBus eventBus, TrackRepository trackRepository,
                                           AccountOperations accountOperations, PlayQueueManager playQueueManager,
                                           AppboyPlaySessionState appboyPlaySessionState,
                                           StopReasonProvider stopReasonProvider, UuidProvider uuidProvider,
                                           @Named(CURRENT_DATE_PROVIDER) DateProvider dateProvider,
                                           @Named(LOW_PRIORITY) Scheduler scheduler) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
        this.appboyPlaySessionState = appboyPlaySessionState;
        this.stopReasonProvider = stopReasonProvider;
        this.uuidProvider = uuidProvider;
        this.dateProvider = dateProvider;
        this.scheduler = scheduler;
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
        checkpointSubscription.unsubscribe();
    }

    private void loadTrackIfChanged(PlaybackStateTransition transition, boolean isNewItem) {
        if (isNewItem) {
            trackObservable = ReplaySubject.createWithSize(1);
            trackRepository.track(transition.getUrn()).subscribe(trackObservable);
        }
    }

    @Override
    public void onSkipTransition(PlaybackStateTransition transition) {
        publishStopEvent(transition, PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    private void publishPlayEvent(final PlaybackStateTransition stateTransition) {
        currentTrackSourceInfo = Optional.fromNullable(playQueueManager.getCurrentTrackSourceInfo());
        if (currentTrackSourceInfo.isPresent() && lastEventIsNotPlay()) {
            trackObservable
                    .map(stateTransitionToSessionPlayEvent(stateTransition))
                    .subscribe(eventBus.queue(EventQueue.TRACKING));

            publishCheckpointEvent(stateTransition);
        }
    }

    private void publishCheckpointEvent(PlaybackStateTransition stateTransition) {
        checkpointSubscription.unsubscribe();
        checkpointSubscription = Observable
                .interval(CHECKPOINT_INTERVAL, TimeUnit.SECONDS, scheduler)
                .flatMap(continueWith(trackObservable))
                .map(stateTransitionToCheckpointEvent(stateTransition))
                .subscribe(eventBus.queue(EventQueue.TRACKING));
    }

    private Func1<PropertySet, TrackingEvent> stateTransitionToCheckpointEvent(final PlaybackStateTransition stateTransition) {
        return new Func1<PropertySet, TrackingEvent>() {
            @Override
            public TrackingEvent call(PropertySet track) {
                return PlaybackSessionEvent.forCheckpoint(buildEventArgs(track, stateTransition));
            }
        };
    }

    private boolean lastEventIsNotPlay() {
        return !(lastPlaySessionEvent.isPresent() && lastPlaySessionEvent.get().isPlayEvent());
    }

    private Func1<PropertySet, PlaybackSessionEvent> stateTransitionToSessionPlayEvent(final PlaybackStateTransition stateTransition) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                PlaybackSessionEvent playSessionEvent = PlaybackSessionEvent.forPlay(buildEventArgs(track, stateTransition));

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

    @NonNull
    private PlaybackSessionEventArgs buildEventArgs(PropertySet track, PlaybackStateTransition stateTransition) {
        return PlaybackSessionEventArgs.create(track,
                accountOperations.getLoggedInUserUrn(),
                currentTrackSourceInfo.get(),
                stateTransition,
                appboyPlaySessionState.isMarketablePlay(),
                uuidProvider.getRandomUuid(),
                dateProvider);
    }

}
