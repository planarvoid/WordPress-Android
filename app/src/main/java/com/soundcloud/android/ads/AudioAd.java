package com.soundcloud.android.ads;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class AudioAd extends PlayerAdData {

    private static AudioAd create(ApiAudioAd apiAudioAd) {
        return new AutoValue_AudioAd(
                apiAudioAd.getUrn(),
                CompanionAd.create(apiAudioAd.getCompanion()),
                apiAudioAd.getTrackingImpressionUrls(),
                apiAudioAd.getTrackingFinishUrls(),
                apiAudioAd.getTrackingSkipUrls()
        );
    }

    public static AudioAd create(ApiAudioAd apiAudioAd, Urn monetizableUrn) {
        AudioAd audioAd = create(apiAudioAd);
        audioAd.setMonetizableTrackUrn(monetizableUrn);
        return audioAd;
    }

}
