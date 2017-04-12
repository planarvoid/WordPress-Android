package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.net.Uri;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class AudioAd extends PlayableAdData {

    public static AudioAd create(ApiModel apiModel, Urn monetizableUrn) {
        final Builder builder = create(apiModel);
        final Optional<ApiCompanionAd> companion = apiModel.companion();
        final AudioAd audioAd = companion.isPresent() ? createWithCompanion(builder, companion.get()).build()
                                                      : builder.build();
        audioAd.setMonetizableTrackUrn(monetizableUrn);
        return audioAd;
    }

    private static AudioAd.Builder createWithCompanion(Builder builder, ApiCompanionAd companion) {
        return builder.getCallToActionButtonText(companion.ctaButtonText)
                      .getDisplayProperties(Optional.of(VisualAdDisplayProperties.create(companion.displayProperties)))
                      .companionAdUrn(Optional.of(companion.urn))
                      .companionImageUrl(Optional.of(Uri.parse(companion.imageUrl)))
                      .clickThroughUrl(extractClickThrough(companion))
                      .getClickUrls(companion.trackingClickUrls)
                      .companionImpressionUrls(companion.trackingImpressionUrls);
    }

    private static AudioAd.Builder create(ApiModel apiModel) {
        final AutoValue_AudioAd.Builder builder = new AutoValue_AudioAd.Builder();
        final ApiAdTracking tracking = apiModel.adTracking();
        return builder.getAdUrn(apiModel.urn())
                      .getMonetizationType(MonetizationType.AUDIO)
                      .getImpressionUrls(tracking.impressionUrls)
                      .getStartUrls(tracking.startUrls)
                      .getFinishUrls(tracking.finishUrls)
                      .getSkipUrls(tracking.skipUrls)
                      .getFirstQuartileUrls(tracking.firstQuartileUrls)
                      .getSecondQuartileUrls(tracking.secondQuartileUrls)
                      .getThirdQuartileUrls(tracking.thirdQuartileUrls)
                      .getPauseUrls(tracking.pauseUrls)
                      .getResumeUrls(tracking.resumeUrls)
                      .isSkippable(apiModel.isSkippable())
                      .getCallToActionButtonText(Optional.absent())
                      .getDisplayProperties(Optional.absent())
                      .getClickUrls(Collections.emptyList())
                      .companionAdUrn(Optional.absent())
                      .companionImageUrl(Optional.absent())
                      .clickThroughUrl(Optional.absent())
                      .companionImpressionUrls(Collections.emptyList())
                      .audioSources(Lists.transform(apiModel.audioSources(), AudioAdSource::create));
    }

    private static Optional<String> extractClickThrough(ApiCompanionAd companion) {
        return Strings.isBlank(companion.clickthroughUrl) ? Optional.absent()
                                                          : Optional.of(companion.clickthroughUrl);
    }

    public boolean hasCompanion() {
        return companionAdUrn().isPresent();
    }

    public abstract Optional<Urn> companionAdUrn();

    public abstract Optional<Uri> companionImageUrl();

    public abstract Optional<String> clickThroughUrl();

    public abstract List<String> companionImpressionUrls();

    public abstract List<AudioAdSource> audioSources();

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder getAdUrn(Urn getAdUrn);
        abstract Builder getMonetizationType(MonetizationType getMonetizationType);

        abstract Builder getCallToActionButtonText(Optional<String> getCallToActionButtonText);
        abstract Builder getImpressionUrls(List<String> getImpressionUrls);
        abstract Builder getStartUrls(List<String> getStartUrls);
        abstract Builder getFinishUrls(List<String> getFinishUrls);
        abstract Builder getSkipUrls(List<String> getSkipUrls);
        abstract Builder getFirstQuartileUrls(List<String> getFirstQuartileUrls);
        abstract Builder getSecondQuartileUrls(List<String> getSecondQuartileUrls);
        abstract Builder getThirdQuartileUrls(List<String> getThirdQuartileUrls);
        abstract Builder getPauseUrls(List<String> getPauseUrls);
        abstract Builder getResumeUrls(List<String> getResumeUrls);
        abstract Builder getClickUrls(List<String> getClickUrls);
        abstract Builder isSkippable(boolean isSkippable);
        abstract Builder getDisplayProperties(Optional<VisualAdDisplayProperties> getDisplayProperties);

        abstract Builder companionAdUrn(Optional<Urn> companionAdUrn);
        abstract Builder companionImageUrl(Optional<Uri> companionImageUrl);
        abstract Builder clickThroughUrl(Optional<String> clickThroughUrl);
        abstract Builder companionImpressionUrls(List<String> companionImpressionUrls);
        abstract Builder audioSources(List<AudioAdSource> audioSources);

        abstract AudioAd build();
    }

    @AutoValue
    abstract static class ApiModel {
        @JsonCreator
        public static ApiModel create(@JsonProperty("urn") Urn urn,
                                      @JsonProperty("skippable") boolean skippable,
                                      @JsonProperty("_embedded") RelatedResources relatedResources,
                                      @JsonProperty("audio_sources") List<AudioAdSource.ApiModel> audioSources,
                                      @JsonProperty("audio_tracking") ApiAdTracking apiAdTracking) {
            return new AutoValue_AudioAd_ApiModel(urn, audioSources, skippable, apiAdTracking, relatedResources.leaveBehind(), relatedResources.companion());
        }

        public abstract Urn urn();
        public abstract List<AudioAdSource.ApiModel> audioSources();
        public abstract boolean isSkippable();
        public abstract ApiAdTracking adTracking();
        public abstract Optional<ApiLeaveBehind> leaveBehind();
        public abstract Optional<ApiCompanionAd> companion();

        @AutoValue
        abstract static class RelatedResources {
            @JsonCreator
            public static RelatedResources create(@JsonProperty("visual_ad") Optional<ApiCompanionAd> companion,
                                                  @JsonProperty("leave_behind") Optional<ApiLeaveBehind> leaveBehind) {
                return new AutoValue_AudioAd_ApiModel_RelatedResources(companion, leaveBehind);
            }

            public abstract Optional<ApiCompanionAd> companion();
            public abstract Optional<ApiLeaveBehind> leaveBehind();
        }
    }
}
