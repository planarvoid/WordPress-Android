package com.soundcloud.android.ads;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class AudioAd extends PlayerAdData {

    private static AudioAd create(ApiAudioAd apiAudioAd) {
        return new AutoValue_AudioAd(
                apiAudioAd.getUrn(),
                apiAudioAd.getTrackingImpressionUrls(),
                apiAudioAd.getTrackingFinishUrls(),
                apiAudioAd.getTrackingSkipUrls(),
                apiAudioAd.isSkippable(),
                VisualAdDisplayProperties.create(apiAudioAd.getCompanion().displayProperties),
                CompanionAd.create(apiAudioAd.getCompanion()),
                apiAudioAd.isThirdParty(),
                apiAudioAd.getApiTrack().getStreamUrl());
    }

    public static AudioAd create(ApiAudioAd apiAudioAd, Urn monetizableUrn) {
        AudioAd audioAd = create(apiAudioAd);
        audioAd.setMonetizableTrackUrn(monetizableUrn);
        return audioAd;
    }

    public abstract CompanionAd getVisualAd();

    public abstract boolean isThirdParty();

    public abstract String getStreamUrl();
}
