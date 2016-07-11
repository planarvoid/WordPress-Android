package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
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

class AdSessionAnalyticsDispatcher implements PlaybackAnalyticsDispatcher {

    static final long CHECKPOINT_INTERVAL = TimeUnit.SECONDS.toMillis(3);

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private final StopReasonProvider stopReasonProvider;
    private final UuidProvider uuidProvider;

    private Optional<PlaybackSessionEvent> lastAudioAdPlaySessionEvent = Optional.absent();
    private Optional<AdData> lastPlayedAd = Optional.absent();
    private Optional<TrackSourceInfo> currentTrackSourceInfo = Optional.absent();
    private ReplaySubject<PropertySet> trackObservable;

    @Inject
    public AdSessionAnalyticsDispatcher(EventBus eventBus, TrackRepository trackRepository,
                                        PlayQueueManager playQueueManager, AdsOperations adsOperations,
                                        StopReasonProvider stopReasonProvider, UuidProvider uuidProvider) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.stopReasonProvider = stopReasonProvider;
        this.uuidProvider = uuidProvider;
    }

    @Override
    public void onProgressEvent(PlaybackProgressEvent progressEvent) {
        if (lastPlayedAd.isPresent() && currentTrackSourceInfo.isPresent()) {
            final PlayerAdData adData = (PlayerAdData) lastPlayedAd.get();
            final PlaybackProgress progress = progressEvent.getPlaybackProgress();
            if (!adData.hasReportedFirstQuartile() && progress.isPastFirstQuartile()) {
                adData.setFirstQuartileReported();
                publishAdQuartileEvent(AdPlaybackSessionEvent.forFirstQuartile(adData, currentTrackSourceInfo.get()));
            } else if (!adData.hasReportedSecondQuartile() && progress.isPastSecondQuartile()) {
                adData.setSecondQuartileReported();
                publishAdQuartileEvent(AdPlaybackSessionEvent.forSecondQuartile(adData, currentTrackSourceInfo.get()));
            } else if (!adData.hasReportedThirdQuartile() && progress.isPastThirdQuartile()) {
                adData.setThirdQuartileReported();
                publishAdQuartileEvent(AdPlaybackSessionEvent.forThirdQuartile(adData, currentTrackSourceInfo.get()));
            }
        }
    }

    @Override
    public void onProgressCheckpoint(PlayStateEvent previousPlayStateEvent, PlaybackProgressEvent progressEvent) {
        if (isCurrentlyPlayingAudioAd()) {
            trackObservable
                    .filter(shouldPublishCheckpoint(progressEvent))
                    .map(toCheckpointEvent(previousPlayStateEvent, progressEvent))
                    .subscribe(eventBus.queue(EventQueue.TRACKING));
        }
    }

    private boolean isCurrentlyPlayingAudioAd() {
        // Checkpoint events are not supported for video ads yet!
        return trackObservable != null;
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

    private void loadTrackIfChanged(PlayStateEvent playStateEvent, boolean isNewItem) {
        if (isNewItem && isCurrentItemTrack()) {
            trackObservable = ReplaySubject.createWithSize(1);
            trackRepository.track(playStateEvent.getPlayingItemUrn()).subscribe(trackObservable);
        }
    }

    @Override
    public void onSkipTransition(PlayStateEvent playStateEvent) {
        publishStopEvent(playStateEvent, PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    private void publishAdQuartileEvent(AdPlaybackSessionEvent adPlaybackSessionEvent) {
        eventBus.publish(EventQueue.TRACKING, adPlaybackSessionEvent);
    }

    private boolean isCurrentItemTrack() {
        return !adsOperations.isCurrentItemVideoAd();
    }

    private void publishPlayEvent(final PlayStateEvent playStateEvent) {
        currentTrackSourceInfo = Optional.fromNullable(playQueueManager.getCurrentTrackSourceInfo());
        if (currentTrackSourceInfo.isPresent() && lastEventWasNotPlayEvent()) {
            if (adsOperations.isCurrentItemVideoAd()) {
                publishVideoAdPlay();
            } else if (adsOperations.isCurrentItemAudioAd()) {
                final AudioAd audioAd = (AudioAd) playQueueManager.getCurrentPlayQueueItem().getAdData().get();
                lastPlayedAd = Optional.<AdData>of(audioAd);
                trackObservable
                        .map(toAudioAdSessionPlayEvent(playStateEvent, audioAd))
                        .subscribe(eventBus.queue(EventQueue.TRACKING));
            }
        }
    }

    private Func1<PropertySet, Boolean> shouldPublishCheckpoint(final PlaybackProgressEvent progressEvent) {
        return new Func1<PropertySet, Boolean>() {
            @Override
            public Boolean call(PropertySet propertyBindings) {
                return lastPlayedAd.isPresent()
                        && lastAudioAdPlaySessionEvent.isPresent()
                        && !lastAudioAdPlaySessionEvent.get().isThirdPartyAd()
                        && lastAudioAdPlaySessionEvent.get().getTrackUrn().equals(progressEvent.getUrn());
            }
        };
    }

    private Func1<PropertySet, PlaybackSessionEvent> toCheckpointEvent(final PlayStateEvent playStateEvent,
                                                                       final PlaybackProgressEvent progressEvent) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet trackPropertySet) {
                return PlaybackSessionEvent.forCheckpoint(buildEventArgs(trackPropertySet,
                                                                         progressEvent.getPlaybackProgress(),
                                                                         playStateEvent.getTransition()))
                                           .withAudioAd((AudioAd) lastPlayedAd.get());
            }
        };
    }

    private void publishVideoAdPlay() {
        lastPlayedAd = adsOperations.getCurrentTrackAdData();
        final PlayerAdData videoAd = (PlayerAdData) lastPlayedAd.get();
        eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forPlay(videoAd, currentTrackSourceInfo.get()));
        videoAd.setStartReported();
    }

    private boolean lastEventWasNotPlayEvent() {
        return !(lastAudioAdPlaySessionEvent.isPresent() && lastAudioAdPlaySessionEvent.get()
                                                                                       .isPlayEvent()) && !lastPlayedAd.isPresent();
    }

    private Func1<PropertySet, PlaybackSessionEvent> toAudioAdSessionPlayEvent(final PlayStateEvent stateTransition, final AudioAd audioAd) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                PlaybackSessionEvent playSessionEventData = PlaybackSessionEvent.forPlay(
                        buildEventArgs(track, stateTransition.getTransition()));
                playSessionEventData = playSessionEventData.withAudioAd(audioAd);
                lastAudioAdPlaySessionEvent = Optional.of(playSessionEventData);
                audioAd.setStartReported();
                return playSessionEventData;
            }
        };
    }

    private void publishStopEvent(final PlayStateEvent playStateEvent, final int stopReason) {
        // note that we only want to publish a stop event if we have a corresponding play event. This value
        // will be nulled out after it is used, and we will not publish another stop event until a play event
        // creates a new value for lastSessionEventData
        if ((lastAudioAdPlaySessionEvent.isPresent() || lastPlayedAd.isPresent()) && currentTrackSourceInfo.isPresent()) {
            if (lastPlayedAd.isPresent() && lastPlayedAd.get() instanceof VideoAd) {
                eventBus.publish(EventQueue.TRACKING,
                                 AdPlaybackSessionEvent.forStop((VideoAd) lastPlayedAd.get(),
                                                                currentTrackSourceInfo.get(),
                                                                stopReason));
            } else {
                final PlaybackSessionEvent lastPlayForStop = lastAudioAdPlaySessionEvent.get();
                final AudioAd audioAd = (AudioAd) lastPlayedAd.get();
                trackObservable
                        .map(toAudioAdSessionStopEvent(stopReason, playStateEvent, lastPlayForStop, audioAd))
                        .subscribe(eventBus.queue(EventQueue.TRACKING));
            }
            lastAudioAdPlaySessionEvent = Optional.absent();
            lastPlayedAd = Optional.absent();
        }
    }

    private Func1<PropertySet, PlaybackSessionEvent> toAudioAdSessionStopEvent(final int stopReason,
                                                                               final PlayStateEvent playStateEvent,
                                                                               final PlaybackSessionEvent lastPlayEventData,
                                                                               final AudioAd audioAd) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                return PlaybackSessionEvent.forStop(
                        lastPlayEventData,
                        stopReason,
                        buildEventArgs(track, playStateEvent.getTransition())
                ).withAudioAd(audioAd);
            }
        };
    }

    @NonNull
    private PlaybackSessionEventArgs buildEventArgs(PropertySet track, PlaybackStateTransition stateTransition) {
        return buildEventArgs(track, stateTransition.getProgress(), stateTransition);
    }

    @NonNull
    private PlaybackSessionEventArgs buildEventArgs(PropertySet track,
                                                    PlaybackProgress playbackProgress,
                                                    PlaybackStateTransition stateTransition) {
        return PlaybackSessionEventArgs.createWithProgress(
                track,
                currentTrackSourceInfo.get(),
                playbackProgress,
                stateTransition,
                false,
                uuidProvider.getRandomUuid());
    }

}
