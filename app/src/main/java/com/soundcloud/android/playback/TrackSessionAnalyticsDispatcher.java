package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.StopReasonProvider.StopReason.*;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
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
    private ReplaySubject<Track> trackObservable;

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
        publishStopEvent(playStateEvent, STOP_REASON_SKIP);
    }

    @Override
    public void onProgressCheckpoint(PlayStateEvent previousPlayStateEvent,
                                     final PlaybackProgressEvent progressEvent) {
        trackObservable
                .filter(track -> isForPlayingTrack(progressEvent))
                .map(stateTransitionToCheckpointEvent(previousPlayStateEvent, progressEvent))
                .subscribe(eventBus.queue(EventQueue.TRACKING));
    }

    private void loadTrackIfChanged(PlayStateEvent playStateEvent, boolean isNewItem) {
        if (isNewItem) {
            trackObservable = ReplaySubject.createWithSize(1);
            trackRepository.track(playStateEvent.getPlayingItemUrn()).filter(track -> track != null).subscribe(trackObservable);
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
        return !(lastPlaySessionEvent.isPresent() && lastPlaySessionEvent.get().isPlayOrPlayStartEvent());
    }

    private Func1<Track, PlaybackSessionEvent> playStateToSessionPlayEvent(final PlayStateEvent playStateEvent) {
        return track -> {
            final String playId = playStateEvent.getPlayId();
            PlaybackSessionEvent playSessionEvent = playStateEvent.isFirstPlay() ?
                                                    PlaybackSessionEvent.forPlayStart(buildEventArgs(track,
                                                                                                     playStateEvent,
                                                                                                     playId,
                                                                                                     playId)) :
                                                    PlaybackSessionEvent.forPlay(buildEventArgs(track,
                                                                                                playStateEvent,
                                                                                                uuidProvider.getRandomUuid(),
                                                                                                playId));

            final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
            PlaySessionSource playSource = playQueueManager.getCurrentPlaySessionSource();

            if (currentPlayQueueItem.isTrack()
                    && playQueueManager.isTrackFromCurrentPromotedItem(currentPlayQueueItem.getUrn())
                    && !playSource.getPromotedSourceInfo().isPlaybackStarted()) {
                PromotedSourceInfo promotedSourceInfo = playSource.getPromotedSourceInfo();
                playSessionEvent = PlaybackSessionEvent.copyWithPromotedTrack(playSessionEvent, promotedSourceInfo);
                promotedSourceInfo.setPlaybackStarted();
            }

            lastPlaySessionEvent = Optional.of(playSessionEvent);
            return playSessionEvent;
        };
    }

    private void publishStopEvent(final PlayStateEvent playStateEvent, final StopReasonProvider.StopReason stopReason) {
        // note that we only want to publish a stop event if we have a corresponding play event. This value
        // will be nulled out after it is used, and we will not publish another stop event until a play event
        // creates a new value for lastSessionEventData
        if (lastPlaySessionEvent.isPresent() && currentTrackSourceInfo.isPresent()) {
            final PlaybackSessionEvent playEventForStop = lastPlaySessionEvent.get();
            trackObservable
                    .map(track -> PlaybackSessionEvent.forStop(playEventForStop,
                                                       stopReason,
                                                       buildEventArgs(track,
                                                                       playStateEvent,
                                                                       uuidProvider.getRandomUuid(),
                                                                       playStateEvent.getPlayId())))
                    .subscribe(eventBus.queue(EventQueue.TRACKING));
            lastPlaySessionEvent = Optional.absent();
        }
    }

    private Func1<Track, TrackingEvent> stateTransitionToCheckpointEvent(final PlayStateEvent playStateEvent,
                                                                             final PlaybackProgressEvent progressEvent) {
        return track -> PlaybackSessionEvent.forCheckpoint(buildEventArgs(track,
                                                                  progressEvent.getPlaybackProgress(),
                                                                  playStateEvent,
                                                                  uuidProvider.getRandomUuid(),
                                                                  playStateEvent.getPlayId()));
    }

    private boolean isForPlayingTrack(PlaybackProgressEvent progressEvent) {
        return lastPlaySessionEvent.isPresent() && lastPlaySessionEvent.get()
                                                                       .trackUrn()
                                                                       .equals(progressEvent.getUrn());
    }

    @NonNull
    private PlaybackSessionEventArgs buildEventArgs(Track track, PlayStateEvent playStateEvent, String clientId, String playId) {
        return buildEventArgs(track, playStateEvent.getProgress(), playStateEvent, clientId, playId);
    }

    private PlaybackSessionEventArgs buildEventArgs(Track track,
                                                    PlaybackProgress progress,
                                                    PlayStateEvent playStateEvent,
                                                    String clientId,
                                                    String playId) {
        return PlaybackSessionEventArgs.createWithProgress(TrackItem.from(track),
                                                           currentTrackSourceInfo.get(),
                                                           progress,
                                                           playStateEvent.getTransition(),
                                                           appboyPlaySessionState.isMarketablePlay(),
                                                           clientId,
                                                           playId);
    }

}
