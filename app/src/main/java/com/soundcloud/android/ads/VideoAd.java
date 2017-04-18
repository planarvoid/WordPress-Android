package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

import java.util.List;
import java.util.UUID;

@AutoValue
public abstract class VideoAd extends PlayableAdData implements ExpirableAd {

    public static VideoAd create(ApiModel apiModel, long createdAt, MonetizationType monetizationType) {
        final ApiAdTracking videoTracking = apiModel.videoTracking();
        return new AutoValue_VideoAd(
                apiModel.adUrn(),
                monetizationType,
                apiModel.callToActionButtonText(),
                videoTracking.impressionUrls(),
                videoTracking.startUrls(),
                videoTracking.finishUrls(),
                videoTracking.skipUrls(),
                videoTracking.firstQuartileUrls(),
                videoTracking.secondQuartileUrls(),
                videoTracking.thirdQuartileUrls(),
                videoTracking.pauseUrls(),
                videoTracking.resumeUrls(),
                videoTracking.clickUrls(),
                apiModel.isSkippable(),
                Optional.of(VisualAdDisplayProperties.create(apiModel.displayProperties())),
                apiModel.uuid(),
                apiModel.title(),
                createdAt,
                apiModel.expiryInMins(),
                apiModel.duration(),
                Lists.transform(apiModel.videoSources(), VideoAdSource::create),
                apiModel.clickThroughUrl(),
                videoTracking.muteUrls(),
                videoTracking.unmuteUrls(),
                videoTracking.fullScreenUrls(),
                videoTracking.exitFullScreenUrls());
    }

    static VideoAd createWithMonetizableTrack(ApiModel apiModel, long createdAt, Urn monetizableTrackUrn) {
        VideoAd videoAd = create(apiModel, createdAt, MonetizationType.VIDEO);
        videoAd.setMonetizableTrackUrn(monetizableTrackUrn);
        return videoAd;
    }

    public abstract String uuid();

    public abstract Optional<String> title();

    public abstract long createdAt();

    public abstract int expiryInMins();

    public abstract long duration();

    public abstract List<VideoAdSource> videoSources();

    public abstract String clickThroughUrl();

    public abstract List<String> muteUrls();

    public abstract List<String> unmuteUrls();

    public abstract List<String> fullScreenUrls();

    public abstract List<String> exitFullScreenUrls();

    public VideoAdSource firstVideoSource() {
        return videoSources().get(0);
    }

    public boolean isVerticalVideo() {
        final VideoAdSource source = firstVideoSource();
        return source.height() > source.width();
    }

    float videoProportion() {
        final VideoAdSource source = firstVideoSource();
        return (float) source.height() / (float) source.width();
    }

    @AutoValue
    abstract static class ApiModel {
        @JsonCreator
        public static ApiModel create(@JsonProperty("urn") Urn adUrn,
                                      @JsonProperty("expiry_in_minutes") int expiryInMins,
                                      @JsonProperty("duration") long duration,
                                      @JsonProperty("title") Optional<String> title,
                                      @JsonProperty("cta_button_text") Optional<String> ctaButtonText,
                                      @JsonProperty("clickthrough_url") String clickthroughUrl,
                                      @JsonProperty("display_properties") ApiDisplayProperties displayProperties,
                                      @JsonProperty("video_sources") List<VideoAdSource.ApiModel> videoSources,
                                      @JsonProperty("video_tracking") ApiAdTracking videoTracking,
                                      @JsonProperty("skippable") boolean skippable) {
            return new AutoValue_VideoAd_ApiModel(adUrn, expiryInMins, duration, UUID.randomUUID().toString(), title, ctaButtonText,
                                                  clickthroughUrl, displayProperties, videoSources, videoTracking, skippable);
        }

        public abstract Urn adUrn();
        public abstract int expiryInMins();
        public abstract long duration();
        public abstract String uuid();
        public abstract Optional<String> title();
        public abstract Optional<String> callToActionButtonText();
        public abstract String clickThroughUrl();
        public abstract ApiDisplayProperties displayProperties();
        public abstract List<VideoAdSource.ApiModel> videoSources();
        public abstract ApiAdTracking videoTracking();
        public abstract boolean isSkippable();
    }
}
