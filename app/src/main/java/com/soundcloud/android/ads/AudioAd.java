package com.soundcloud.android.ads;

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

    private static AudioAd create(ApiAudioAd apiAudioAd) {
        final ApiAdTracking adTracking = apiAudioAd.getApiAdTracking();
        return new AutoValue_AudioAd(
                apiAudioAd.getUrn(),
                adTracking.impressionUrls,
                adTracking.startUrls,
                adTracking.finishUrls,
                adTracking.skipUrls,
                adTracking.firstQuartileUrls,
                adTracking.secondQuartileUrls,
                adTracking.thirdQuartileUrls,
                adTracking.pauseUrls,
                adTracking.resumeUrls,
                apiAudioAd.hasCompanion() ? apiAudioAd.getCompanion().trackingClickUrls : Collections.<String>emptyList(),
                apiAudioAd.isSkippable(),
                extractVisualAdDisplayProperties(apiAudioAd),
                apiAudioAd.hasCompanion() ? Optional.of(apiAudioAd.getCompanion().urn) : Optional.<Urn>absent(),
                apiAudioAd.hasCompanion() ? Optional.of(Uri.parse(apiAudioAd.getCompanion().imageUrl)): Optional.<Uri>absent(),
                extractClickThrough(apiAudioAd),
                apiAudioAd.hasCompanion() ? apiAudioAd.getCompanion().ctaButtonText : Optional.<String>absent(),
                apiAudioAd.hasCompanion() ? apiAudioAd.getCompanion().trackingImpressionUrls : Collections.<String>emptyList(),
                Lists.transform(apiAudioAd.getAudioSources(), ApiAudioAdSource.toAudioAdSource));
    }

    public static AudioAd create(ApiAudioAd apiAudioAd, Urn monetizableUrn) {
        AudioAd audioAd = create(apiAudioAd);
        audioAd.setMonetizableTrackUrn(monetizableUrn);
        return audioAd;
    }

    private static Optional<VisualAdDisplayProperties> extractVisualAdDisplayProperties(ApiAudioAd apiAudioAd) {
        return apiAudioAd.hasCompanion()
                ? Optional.of(VisualAdDisplayProperties.create(apiAudioAd.getCompanion().displayProperties))
                : Optional.<VisualAdDisplayProperties>absent();
    }

    private static Optional<String> extractClickThrough(ApiAudioAd audioAd) {
        if (audioAd.hasCompanion()) {
            final ApiCompanionAd companion = audioAd.getCompanion();
            return Strings.isBlank(companion.clickthroughUrl)
                    ? Optional.<String>absent()
                    : Optional.of(companion.clickthroughUrl);
        } else {
            return Optional.absent();
        }
    }

    public boolean hasCompanion() {
        return getCompanionAdUrn().isPresent();
    }

    public abstract Optional<Urn> getCompanionAdUrn();

    public abstract Optional<Uri> getCompanionImageUrl();

    public abstract Optional<String> getClickThroughUrl();

    public abstract Optional<String> getCallToActionButtonText();

    public abstract List<String> getCompanionImpressionUrls();

    public abstract List<AudioAdSource> getAudioSources();
}
