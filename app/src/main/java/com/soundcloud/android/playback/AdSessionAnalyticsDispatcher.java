package com.soundcloud.android.playback;

import static com.soundcloud.android.ads.PlayableAdData.ReportingEvent;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason;

import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdSessionEventArgs;
import com.soundcloud.android.events.AdRichMediaSessionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import javax.annotation.Nullable;
import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

class AdSessionAnalyticsDispatcher implements PlaybackAnalyticsDispatcher {

    static final long CHECKPOINT_INTERVAL = TimeUnit.SECONDS.toMillis(3);

    private final EventBus eventBus;
    private final StopReasonProvider stopReasonProvider;

    private Optional<AdInfo> adInfo = Optional.absent();
    private boolean lastEventWasPlay;

    @Inject
    public AdSessionAnalyticsDispatcher(EventBus eventBus, StopReasonProvider stopReasonProvider) {
        this.eventBus = eventBus;
        this.stopReasonProvider = stopReasonProvider;
    }

    public void setAdMetadata(PlayableAdData ad, @Nullable TrackSourceInfo sourceInfo) {
        if (sourceInfo != null) {
            adInfo = Optional.of(new AdInfo(ad, sourceInfo));
        }
    }

    @Override
    public void onPlayTransition(PlayStateEvent playStateEvent, boolean isNewItem) {
        if (adInfo.isPresent() && !lastEventWasPlay) {
            final PlayableAdData ad = adInfo.get().ad;
            final TrackSourceInfo sourceInfo = adInfo.get().sourceInfo;
            final AdSessionEventArgs eventArgs = buildArgs(sourceInfo, playStateEvent.getTransition());
            sendPlayTrackingEvent(ad, eventArgs);
            ad.setEventReported(ReportingEvent.START_EVENT);
        }
    }

    @Override
    public void onStopTransition(PlayStateEvent playStateEvent, boolean isNewItem) {
        if (adInfo.isPresent() && lastEventWasPlay) {
            final PlaybackStateTransition transition = playStateEvent.getTransition();
            final AdInfo adInfo = this.adInfo.get();
            sendStopTrackingEvent(adInfo.ad, buildArgs(adInfo.sourceInfo, transition), stopReasonProvider.fromTransition(transition));
        }
    }

    @Override
    public void onSkipTransition(PlayStateEvent playStateEvent) {
        if (adInfo.isPresent() && lastEventWasPlay) {
            final AdInfo adInfo = this.adInfo.get();
            sendStopTrackingEvent(adInfo.ad, buildArgs(adInfo.sourceInfo, playStateEvent.getTransition()), StopReason.STOP_REASON_SKIP);
        }
    }

    @Override
    public void onProgressCheckpoint(PlayStateEvent previousStateEvent, PlaybackProgressEvent progressEvent) {
        if (adInfo.isPresent() && lastEventWasPlay && previousStateEvent.getPlayingItemUrn().equals(progressEvent.getUrn())) {
            final AdInfo adInfo = this.adInfo.get();
            final AdSessionEventArgs eventArgs = buildArgs(adInfo.sourceInfo, previousStateEvent.getTransition(), progressEvent.getPlaybackProgress());
            eventBus.publish(EventQueue.TRACKING, AdRichMediaSessionEvent.forCheckpoint(adInfo.ad, eventArgs));
        }
    }

    @Override
    public void onProgressEvent(PlaybackProgressEvent progressEvent) {
        if (adInfo.isPresent()) {
            final PlayableAdData ad = adInfo.get().ad;
            final TrackSourceInfo sourceInfo = adInfo.get().sourceInfo;
            final PlaybackProgress progress = progressEvent.getPlaybackProgress();

            if (shouldReportQuartileEvent(ReportingEvent.FIRST_QUARTILE, ad, progress)) {
                reportQuartileEvent(ReportingEvent.FIRST_QUARTILE, ad, sourceInfo);
            } else if (shouldReportQuartileEvent(ReportingEvent.SECOND_QUARTILE, ad, progress)) {
                reportQuartileEvent(ReportingEvent.SECOND_QUARTILE, ad, sourceInfo);
            } else if (shouldReportQuartileEvent(ReportingEvent.THIRD_QUARTILE, ad, progress)) {
                reportQuartileEvent(ReportingEvent.THIRD_QUARTILE, ad, sourceInfo);
            }
        }
    }

    private boolean shouldReportQuartileEvent(ReportingEvent event, PlayableAdData ad, PlaybackProgress progress) {
        final boolean hasReportedEvent = ad.hasReportedEvent(event);
        switch (event) {
            case FIRST_QUARTILE:
                return !hasReportedEvent && progress.isPastFirstQuartile();
            case SECOND_QUARTILE:
                return !hasReportedEvent && progress.isPastSecondQuartile();
            case THIRD_QUARTILE:
                return !hasReportedEvent && progress.isPastThirdQuartile();
            default:
                return false;
        }
    }

    private void reportQuartileEvent(ReportingEvent event, PlayableAdData ad, TrackSourceInfo sourceInfo) {
        ad.setEventReported(event);
        switch (event) {
            case FIRST_QUARTILE:
                eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forFirstQuartile(ad, sourceInfo));
                break;
            case SECOND_QUARTILE:
                eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forSecondQuartile(ad, sourceInfo));
                break;
            case THIRD_QUARTILE:
                eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forThirdQuartile(ad, sourceInfo));
                break;
        }
    }

    private void sendPlayTrackingEvent(PlayableAdData ad, AdSessionEventArgs args) {
        lastEventWasPlay = true;
        if (AdPlaybackSessionEvent.shouldTrackStart(ad)) {
            eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forPlay(ad, args));
        }
        eventBus.publish(EventQueue.TRACKING, AdRichMediaSessionEvent.forPlay(ad, args));
    }

    private void sendStopTrackingEvent(PlayableAdData ad, AdSessionEventArgs args, StopReason stopReason) {
        lastEventWasPlay = false;
        if (AdPlaybackSessionEvent.shouldTrackFinish(stopReason)) {
            eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forStop(ad, args, stopReason));
        }
        eventBus.publish(EventQueue.TRACKING, AdRichMediaSessionEvent.forStop(ad, args, stopReason));
    }

    private AdSessionEventArgs buildArgs(TrackSourceInfo sourceInfo, PlaybackStateTransition transition) {
        return buildArgs(sourceInfo, transition, transition.getProgress());
    }

    private AdSessionEventArgs buildArgs(TrackSourceInfo sourceInfo, PlaybackStateTransition transition, PlaybackProgress progress) {
        return AdSessionEventArgs.createWithProgress(sourceInfo, progress, transition);
    }

    private static class AdInfo {
        final PlayableAdData ad;
        final TrackSourceInfo sourceInfo;

        AdInfo(PlayableAdData adData, TrackSourceInfo sourceInfo) {
            this.ad = adData;
            this.sourceInfo = sourceInfo;
        }
    }
}
