package com.soundcloud.android.playback;

import static com.soundcloud.android.ApplicationModule.CURRENT_DATE_PROVIDER;

import com.soundcloud.android.accounts.AccountOperations;
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
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;

class AdSessionAnalyticsDispatcher implements PlaybackAnalyticsDispatcher {

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final AccountOperations accountOperations;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private final StopReasonProvider stopReasonProvider;
    private final UuidProvider uuidProvider;
    private final DateProvider dateProvider;

    private Optional<PlaybackSessionEvent> lastAudioAdPlaySessionEvent = Optional.absent();
    private Optional<AdData> lastPlayedAd = Optional.absent();
    private Optional<TrackSourceInfo> currentTrackSourceInfo = Optional.absent();
    private ReplaySubject<PropertySet> trackObservable;

    @Inject
    public AdSessionAnalyticsDispatcher(EventBus eventBus, TrackRepository trackRepository,
                                        AccountOperations accountOperations, PlayQueueManager playQueueManager,
                                        AdsOperations adsOperations, StopReasonProvider stopReasonProvider,
                                        UuidProvider uuidProvider, @Named(CURRENT_DATE_PROVIDER) DateProvider dateProvider) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.stopReasonProvider = stopReasonProvider;
        this.uuidProvider = uuidProvider;
        this.dateProvider = dateProvider;
    }

    @Override
    public void onProgressEvent(PlaybackProgressEvent progressEvent) {
        final PlayerAdData adData = (PlayerAdData) adsOperations.getCurrentTrackAdData().get();
        final PlaybackProgress progress = progressEvent.getPlaybackProgress();
        final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        if (!adData.hasReportedFirstQuartile() && progress.isPastFirstQuartile()) {
            adData.setFirstQuartileReported();
            publishAdQuartileEvent(AdPlaybackSessionEvent.forFirstQuartile(adData, trackSourceInfo));
        } else if (!adData.hasReportedSecondQuartile() && progress.isPastSecondQuartile()) {
            adData.setSecondQuartileReported();
            publishAdQuartileEvent(AdPlaybackSessionEvent.forSecondQuartile(adData, trackSourceInfo));
        } else if (!adData.hasReportedThirdQuartile() && progress.isPastThirdQuartile()) {
            adData.setThirdQuartileReported();
            publishAdQuartileEvent(AdPlaybackSessionEvent.forThirdQuartile(adData, trackSourceInfo));
        }
    }

    @Override
    public void onPlayTransition(PlaybackStateTransition transition, boolean isNewItem) {
        if (isNewItem && isCurrentItemTrack()) {
            trackObservable = ReplaySubject.createWithSize(1);
            trackRepository.track(transition.getUrn()).subscribe(trackObservable);
        }
        publishPlayEvent(transition);
    }

    @Override
    public void onStopTransition(PlaybackStateTransition transition) {
        publishStopEvent(transition, stopReasonProvider.fromTransition(transition));
    }

    @Override
    public void onSkipTransition(PlaybackStateTransition transition) {
        publishStopEvent(transition, PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    private void publishAdQuartileEvent(AdPlaybackSessionEvent adPlaybackSessionEvent) {
        eventBus.publish(EventQueue.TRACKING, adPlaybackSessionEvent);
    }

    private boolean isCurrentItemTrack() {
        return !adsOperations.isCurrentItemVideoAd();
    }

    private void publishPlayEvent(final PlaybackStateTransition stateTransition) {
        currentTrackSourceInfo = Optional.fromNullable(playQueueManager.getCurrentTrackSourceInfo());
        if (currentTrackSourceInfo.isPresent() && lastEventWasNotPlayEvent()) {
            if (adsOperations.isCurrentItemVideoAd()) {
                lastPlayedAd = adsOperations.getCurrentTrackAdData();
                eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forPlay((PlayerAdData) lastPlayedAd.get(), currentTrackSourceInfo.get(), stateTransition));
            } else {
                trackObservable
                        .map(toAudioAdSessionPlayEvent(stateTransition))
                        .subscribe(eventBus.queue(EventQueue.TRACKING));
            }
        }
    }

    private boolean lastEventWasNotPlayEvent() {
        return !(lastAudioAdPlaySessionEvent.isPresent() && lastAudioAdPlaySessionEvent.get().isPlayEvent()) && !lastPlayedAd.isPresent();
    }

    private Func1<PropertySet, PlaybackSessionEvent> toAudioAdSessionPlayEvent(final PlaybackStateTransition stateTransition) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                PlaybackSessionEvent playSessionEventData = PlaybackSessionEvent.forPlay(
                        buildEventArgs(track, stateTransition));

                lastPlayedAd = playQueueManager.getCurrentPlayQueueItem().getAdData();
                playSessionEventData = playSessionEventData.withAudioAd((AudioAd) lastPlayedAd.get());
                lastAudioAdPlaySessionEvent = Optional.of(playSessionEventData);
                return playSessionEventData;
            }
        };
    }

    private void publishStopEvent(final PlaybackStateTransition stateTransition, final int stopReason) {
        // note that we only want to publish a stop event if we have a corresponding play event. This value
        // will be nulled out after it is used, and we will not publish another stop event until a play event
        // creates a new value for lastSessionEventData
        if ((lastAudioAdPlaySessionEvent.isPresent() || lastPlayedAd.isPresent()) && currentTrackSourceInfo.isPresent()) {
            if (lastPlayedAd.isPresent() && lastPlayedAd.get() instanceof VideoAd) {
                eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forStop((VideoAd) lastPlayedAd.get(), currentTrackSourceInfo.get(), stateTransition, stopReason));
            } else {
                trackObservable
                        .map(toAudioAdSessionStopEvent(stopReason, stateTransition, lastAudioAdPlaySessionEvent.get()))
                        .subscribe(eventBus.queue(EventQueue.TRACKING));
            }
            lastAudioAdPlaySessionEvent = Optional.absent();
            lastPlayedAd = Optional.absent();
        }
    }

    private Func1<PropertySet, PlaybackSessionEvent> toAudioAdSessionStopEvent(final int stopReason, final PlaybackStateTransition stateTransition, final PlaybackSessionEvent lastPlayEventData) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                return PlaybackSessionEvent.forStop(
                        lastPlayEventData,
                        stopReason,
                        buildEventArgs(track, stateTransition)
                ).withAudioAd((AudioAd) lastPlayedAd.get());
            }
        };
    }

    @NonNull
    private PlaybackSessionEventArgs buildEventArgs(PropertySet track, PlaybackStateTransition stateTransition) {
        return PlaybackSessionEventArgs.create(
                track,
                accountOperations.getLoggedInUserUrn(),
                currentTrackSourceInfo.get(),
                stateTransition,
                false,
                uuidProvider.getRandomUuid(),
                dateProvider);
    }

}
