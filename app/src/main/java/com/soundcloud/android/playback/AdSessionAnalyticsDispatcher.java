package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdPlaybackSessionEventArgs;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class AdSessionAnalyticsDispatcher implements PlaybackAnalyticsDispatcher {

    static final long CHECKPOINT_INTERVAL = TimeUnit.SECONDS.toMillis(3);

    private static final Function<AdData, PlayerAdData> TO_PLAYER_AD = adData -> (PlayerAdData) adData;

    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private final StopReasonProvider stopReasonProvider;

    private Optional<PlayerAdData> currentPlayingAd = Optional.absent();
    private Optional<TrackSourceInfo> currentTrackSourceInfo = Optional.absent();

    @Inject
    public AdSessionAnalyticsDispatcher(EventBus eventBus,
                                        PlayQueueManager playQueueManager,
                                        AdsOperations adsOperations,
                                        StopReasonProvider stopReasonProvider) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.stopReasonProvider = stopReasonProvider;
    }

    @Override
    public void onProgressEvent(PlaybackProgressEvent progressEvent) {
        if (currentPlayingAd.isPresent() && currentTrackSourceInfo.isPresent()) {
            final PlayerAdData adData = currentPlayingAd.get();
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
    public void onPlayTransition(PlayStateEvent playStateEvent, boolean isNewItem) {
        currentTrackSourceInfo = Optional.fromNullable(playQueueManager.getCurrentTrackSourceInfo());
        if (!currentPlayingAd.isPresent() && currentTrackSourceInfo.isPresent()) {
            currentPlayingAd = getPlayerAdDataIfAvailable();
            if (currentPlayingAd.isPresent()) {
                final AdPlaybackSessionEventArgs eventArgs = buildEventArgs(playStateEvent.getTransition());
                eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forPlay(currentPlayingAd.get(), eventArgs));
                currentPlayingAd.get().setStartReported();
            } else {
                ErrorUtils.handleSilentException(new IllegalStateException("AdSessionAnalyticsController couldn't retrieve ad data for play state: " + playStateEvent.getTransition()));
            }
        }
    }

    private Optional<PlayerAdData> getPlayerAdDataIfAvailable() {
        Optional<AdData> adData = adsOperations.getCurrentTrackAdData();
        if (adData.isPresent() && adData.get() instanceof PlayerAdData) {
            return adData.transform(TO_PLAYER_AD);
        }
        return Optional.absent();
    }

    @Override
    public void onStopTransition(PlayStateEvent playStateEvent, boolean isNewItem) {
        final PlaybackStateTransition transition = playStateEvent.getTransition();
        publishStopEvent(transition, stopReasonProvider.fromTransition(transition));
    }

    @Override
    public void onSkipTransition(PlayStateEvent playStateEvent) {
        publishStopEvent(playStateEvent.getTransition(), PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    @Override
    public void onProgressCheckpoint(PlayStateEvent previousPlayStateEvent, PlaybackProgressEvent progressEvent) {
        if (currentPlayingAd.isPresent() && previousPlayStateEvent.getPlayingItemUrn().equals(progressEvent.getUrn())) {
            eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forCheckpoint(currentPlayingAd.get(),
                    buildEventArgs(previousPlayStateEvent.getTransition(), progressEvent.getPlaybackProgress())));
        }
    }

    private void publishStopEvent(final PlaybackStateTransition stateTransition, final int stopReason) {
        // Note that we only want to publish a stop event if we have a corresponding play event.
        if (currentPlayingAd.isPresent() && currentTrackSourceInfo.isPresent()) {
            eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forStop(currentPlayingAd.get(), buildEventArgs(stateTransition), stopReason));
            currentPlayingAd = Optional.absent();
        }
    }

    private void publishAdQuartileEvent(AdPlaybackSessionEvent adPlaybackSessionEvent) {
        eventBus.publish(EventQueue.TRACKING, adPlaybackSessionEvent);
    }

    private AdPlaybackSessionEventArgs buildEventArgs(PlaybackStateTransition stateTransition) {
        return buildEventArgs(stateTransition, stateTransition.getProgress());
    }

    private AdPlaybackSessionEventArgs buildEventArgs(PlaybackStateTransition stateTransition, PlaybackProgress playbackProgress) {
        return AdPlaybackSessionEventArgs.createWithProgress(currentTrackSourceInfo.get(), playbackProgress, stateTransition,
                                                             UUID.randomUUID().toString());
    }
}
