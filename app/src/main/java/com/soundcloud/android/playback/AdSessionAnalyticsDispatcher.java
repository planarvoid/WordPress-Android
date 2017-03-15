package com.soundcloud.android.playback;

import static com.soundcloud.android.ads.PlayableAdData.ReportingEvent;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason;

import com.soundcloud.android.ads.AdViewabilityController;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
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

public class AdSessionAnalyticsDispatcher implements PlaybackAnalyticsDispatcher {

    public static final long CHECKPOINT_INTERVAL = TimeUnit.SECONDS.toMillis(3);

    private final EventBus eventBus;
    private final StopReasonProvider stopReasonProvider;
    private final AdViewabilityController adViewabilityController;

    private Optional<AdInfo> adInfo = Optional.absent();
    private boolean lastEventWasPlay;

    @Inject
    public AdSessionAnalyticsDispatcher(EventBus eventBus,
                                        StopReasonProvider stopReasonProvider,
                                        AdViewabilityController adViewabilityController) {
        this.eventBus = eventBus;
        this.stopReasonProvider = stopReasonProvider;
        this.adViewabilityController = adViewabilityController;
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

            sendPlayTrackingEvent(ad, playStateEvent.getProgress().getPosition(), eventArgs);
        }
    }

    @Override
    public void onStopTransition(PlayStateEvent playStateEvent, boolean isNewItem) {
        if (adInfo.isPresent() && lastEventWasPlay) {
            final PlayableAdData ad = adInfo.get().ad;
            final TrackSourceInfo sourceInfo = adInfo.get().sourceInfo;
            final PlaybackStateTransition transition = playStateEvent.getTransition();
            final StopReason stopReason = stopReasonProvider.fromTransition(transition);
            final long position = playStateEvent.getProgress().getPosition();
            sendStopTrackingEvent(ad, position, buildArgs(sourceInfo, transition), stopReason);
        }
    }

    @Override
    public void onSkipTransition(PlayStateEvent playStateEvent) {
        if (adInfo.isPresent() && lastEventWasPlay) {
            final AdInfo adInfo = this.adInfo.get();
            final long position = playStateEvent.getProgress().getPosition();
            sendStopTrackingEvent(adInfo.ad, position, buildArgs(adInfo.sourceInfo, playStateEvent.getTransition()), StopReason.STOP_REASON_SKIP);
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
                reportQuartileEvent(ReportingEvent.FIRST_QUARTILE, ad, progress, sourceInfo);
            } else if (shouldReportQuartileEvent(ReportingEvent.SECOND_QUARTILE, ad, progress)) {
                reportQuartileEvent(ReportingEvent.SECOND_QUARTILE, ad, progress, sourceInfo);
            } else if (shouldReportQuartileEvent(ReportingEvent.THIRD_QUARTILE, ad, progress)) {
                reportQuartileEvent(ReportingEvent.THIRD_QUARTILE, ad, progress, sourceInfo);
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

    private void reportQuartileEvent(ReportingEvent event, PlayableAdData ad, PlaybackProgress progress, TrackSourceInfo sourceInfo) {
        ad.setEventReported(event);
        progressViewabilityTracking(ad, event, progress.getPosition());
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

    private void sendPlayTrackingEvent(PlayableAdData ad, long position, AdSessionEventArgs args) {
        lastEventWasPlay = true;

        final AdPlaybackSessionEvent playbackSessionEvent;
        if (shouldTrackStart(ad)) {
            ad.setEventReported(ReportingEvent.START);
            progressViewabilityTracking(ad, ReportingEvent.START, position);
            playbackSessionEvent = AdPlaybackSessionEvent.forStart(ad, args);
        } else {
            resumeViewabilityTracking(ad, position);
            playbackSessionEvent = AdPlaybackSessionEvent.forResume(ad, args);
        }

        eventBus.publish(EventQueue.TRACKING, playbackSessionEvent);
        eventBus.publish(EventQueue.TRACKING, AdRichMediaSessionEvent.forPlay(ad, args));
    }

    private void sendStopTrackingEvent(PlayableAdData ad, long position, AdSessionEventArgs args, StopReason stopReason) {
        lastEventWasPlay = false;

        if (shouldTrackFinish(stopReason, ad)) {
            ad.setEventReported(ReportingEvent.FINISH);
            progressViewabilityTracking(ad, ReportingEvent.FINISH, position);
            eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forFinish(ad, args));
        } else if (shouldTrackPause(stopReason)){
            pauseViewabilityTracking(ad, position);
            eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forPause(ad, args));
        }

        eventBus.publish(EventQueue.TRACKING, AdRichMediaSessionEvent.forStop(ad, args, stopReason));
    }

    private void pauseViewabilityTracking(PlayableAdData ad, long position) {
        if (isVideo(ad)) {
            adViewabilityController.onPaused((VideoAd) ad, position);
        }
    }

    private void resumeViewabilityTracking(PlayableAdData ad, long position) {
        if (isVideo(ad)) {
            adViewabilityController.onResume((VideoAd) ad, position);
        }
    }

    private void progressViewabilityTracking(PlayableAdData ad, ReportingEvent event, long position) {
        if (isVideo(ad)) {
            adViewabilityController.onProgressQuartileEvent((VideoAd) ad, event, position);
        }
    }

    private static boolean shouldTrackStart(PlayableAdData adData) {
        return !adData.hasReportedEvent(ReportingEvent.START);
    }

    private static boolean shouldTrackFinish(StopReasonProvider.StopReason stopReason, PlayableAdData adData) {
        return stopReason == StopReasonProvider.StopReason.STOP_REASON_TRACK_FINISHED && !adData.hasReportedEvent(ReportingEvent.FINISH);
    }

    private static boolean shouldTrackPause(StopReasonProvider.StopReason stopReason) {
        return stopReason == StopReasonProvider.StopReason.STOP_REASON_PAUSE;
    }

    private static boolean isVideo(PlayableAdData adData) {
        return adData instanceof VideoAd;
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
