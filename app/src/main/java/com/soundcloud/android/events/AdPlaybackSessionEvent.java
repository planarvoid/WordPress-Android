package com.soundcloud.android.events;

import static com.soundcloud.android.ads.PlayableAdData.ReportingEvent;
import static com.soundcloud.android.events.AdPlaybackSessionEvent.MonetizationType.MONETIZATION_AUDIO;
import static com.soundcloud.android.events.AdPlaybackSessionEvent.MonetizationType.MONETIZATION_VIDEO;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.StopReasonProvider;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class AdPlaybackSessionEvent extends TrackingEvent {

    public enum EventName {
        CLICK_EVENT("click"),
        IMPRESSION_EVENT("impression");
        private final String key;

        EventName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

    }

    public enum EventKind {
        PLAY("play"),
        STOP("stop"),
        QUARTILE("quartile_event");
        private final String key;

        EventKind(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum ClickName {

        FIRST_QUARTILE_TYPE("ad::first_quartile"),
        SECOND_QUARTILE_TYPE("ad::second_quartile"),
        THIRD_QUARTILE_TYPE("ad::third_quartile"),
        AD_FINISH("ad::finish");

        private final String key;

        ClickName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum ImpressionName {
        VIDEO_AD("video_ad_impression"),
        AUDIO_AD("audio_ad_impression");

        private final String key;

        ImpressionName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum MonetizationType {
        MONETIZATION_AUDIO("audio_ad"),
        MONETIZATION_VIDEO("video_ad");
        private final String key;

        MonetizationType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public abstract Optional<EventName> eventName();

    public abstract EventKind eventKind();

    public abstract Urn adUrn();

    public abstract Urn monetizableTrackUrn();

    public abstract MonetizationType monetizationType();

    public abstract Optional<List<String>> trackingUrls();

    public abstract Optional<ClickName> clickName();

    public abstract Optional<ImpressionName> impressionName();

    public abstract String pageName();

    public abstract Optional<StopReasonProvider.StopReason> stopReason();

    public abstract boolean shouldReportStartWithPlay();

    public static AdPlaybackSessionEvent forFirstQuartile(PlayableAdData adData, TrackSourceInfo trackSourceInfo) {
        return createQuartileEvent(adData, trackSourceInfo, adData.getFirstQuartileUrls(), ClickName.FIRST_QUARTILE_TYPE);
    }

    public static AdPlaybackSessionEvent forSecondQuartile(PlayableAdData adData, TrackSourceInfo trackSourceInfo) {
        return createQuartileEvent(adData, trackSourceInfo, adData.getSecondQuartileUrls(), ClickName.SECOND_QUARTILE_TYPE);
    }

    public static AdPlaybackSessionEvent forThirdQuartile(PlayableAdData adData, TrackSourceInfo trackSourceInfo) {
        return createQuartileEvent(adData, trackSourceInfo, adData.getThirdQuartileUrls(), ClickName.THIRD_QUARTILE_TYPE);
    }

    protected static AdPlaybackSessionEvent createQuartileEvent(PlayableAdData adData,
                                                                TrackSourceInfo trackSourceInfo,
                                                                List<String> quartileUrls, ClickName quartileType) {
        return create(EventKind.QUARTILE, adData, trackSourceInfo.getOriginScreen())
                .eventName(Optional.of(EventName.CLICK_EVENT))
                .trackingUrls(Optional.of(quartileUrls))
                .clickName(Optional.of(quartileType))
                .build();
    }

    public static boolean shouldTrackStart(PlayableAdData adData) {
        return !adData.hasReportedEvent(ReportingEvent.START_EVENT);
    }

    public static AdPlaybackSessionEvent forPlay(PlayableAdData adData, AdSessionEventArgs eventArgs) {
        return AdPlaybackSessionEvent.create(EventKind.PLAY, adData, eventArgs.getTrackSourceInfo().getOriginScreen())
                                     .eventName(Optional.of(EventName.IMPRESSION_EVENT))
                                     .impressionName(adData instanceof VideoAd ? Optional.of(ImpressionName.VIDEO_AD) : Optional.of(ImpressionName.AUDIO_AD))
                                     .trackingUrls(Optional.of(getPlayEventTracking(adData)))
                                     .build();
    }

    public static boolean shouldTrackFinish(StopReasonProvider.StopReason stopReason) {
        return stopReason == StopReasonProvider.StopReason.STOP_REASON_TRACK_FINISHED;
    }

    public static AdPlaybackSessionEvent forStop(PlayableAdData adData, AdSessionEventArgs eventArgs, StopReasonProvider.StopReason stopReason) {
        return AdPlaybackSessionEvent.create(EventKind.STOP, adData, eventArgs.getTrackSourceInfo().getOriginScreen())
                                     .eventName(Optional.of(EventName.CLICK_EVENT))
                                     .clickName(Optional.of(ClickName.AD_FINISH))
                                     .trackingUrls(getStopEventTrackingUrls(adData, stopReason))
                                     .stopReason(Optional.of(stopReason))
                                     .build();
    }

    private static AdPlaybackSessionEvent.Builder create(EventKind kind, PlayableAdData adData, String pageName) {
        final AutoValue_AdPlaybackSessionEvent.Builder builder = new AutoValue_AdPlaybackSessionEvent.Builder();
        builder.id(defaultId())
               .timestamp(defaultTimestamp())
               .referringEvent(Optional.absent())
               .eventKind(kind)
               .eventName(Optional.absent())
               .adUrn(adData.getAdUrn())
               .monetizableTrackUrn(adData.getMonetizableTrackUrn())
               .monetizationType(adData instanceof VideoAd ? MONETIZATION_VIDEO : MONETIZATION_AUDIO)
               .trackingUrls(Optional.absent())
               .shouldReportStartWithPlay(!adData.hasReportedEvent(ReportingEvent.START_EVENT))
               .pageName(pageName)
               .clickName(Optional.absent())
               .impressionName(Optional.absent())
               .stopReason(Optional.absent());
        return builder;
    }

    private static List<String> getPlayEventTracking(PlayableAdData adData) {
        List<String> trackingUrls;
        if (!adData.hasReportedEvent(ReportingEvent.START_EVENT)) {
            trackingUrls = new ArrayList<>();
            trackingUrls.addAll(adData.getImpressionUrls());
            trackingUrls.addAll(adData.getStartUrls());
        } else {
            trackingUrls = adData.getResumeUrls();
        }
        return trackingUrls;
    }

    private static Optional<List<String>> getStopEventTrackingUrls(PlayableAdData adData, StopReasonProvider.StopReason stopReason) {
        if (stopReason == StopReasonProvider.StopReason.STOP_REASON_TRACK_FINISHED) {
            return Optional.of(adData.getFinishUrls());
        } else if (stopReason == StopReasonProvider.StopReason.STOP_REASON_PAUSE) {
            return Optional.of(adData.getPauseUrls());
        }
        return Optional.absent();
    }

    public boolean isVideoAd() {
        return MONETIZATION_VIDEO.equals(monetizationType());
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder id(String id);

        abstract Builder timestamp(long timestamp);

        abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        abstract Builder eventName(Optional<EventName> eventName);

        abstract Builder eventKind(EventKind eventKind);

        abstract Builder adUrn(Urn adUrn);

        abstract Builder monetizableTrackUrn(Urn monetizableTrackUrn);

        abstract Builder monetizationType(MonetizationType monetizationType);

        abstract Builder trackingUrls(Optional<List<String>> trackingUrls);

        abstract Builder clickName(Optional<ClickName> clickName);

        abstract Builder impressionName(Optional<ImpressionName> impressionName);

        abstract Builder pageName(String pageName);

        abstract Builder stopReason(Optional<StopReasonProvider.StopReason> stopReason);

        abstract Builder shouldReportStartWithPlay(boolean shouldReportStartWithPlay);

        abstract AdPlaybackSessionEvent build();
    }

    @Override
    public AdPlaybackSessionEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_AdPlaybackSessionEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }
}
