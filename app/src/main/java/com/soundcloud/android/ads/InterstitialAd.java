package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import android.net.Uri;

import java.util.List;

@AutoValue
public abstract class InterstitialAd extends VisualAdData {

    private static InterstitialAd create(ApiModel apiModel) {
        return new AutoValue_InterstitialAd(apiModel.adUrn(),
                                            MonetizationType.INTERSTITIAL,
                                            apiModel.imageUrl(),
                                            Uri.parse(apiModel.clickthroughUrl()),
                                            apiModel.trackingImpressionUrls(),
                                            apiModel.trackingClickUrls());
    }

    public static InterstitialAd create(ApiModel apiInterstitial, Urn monetizableTrackUrn) {
        final InterstitialAd interstitialAd = create(apiInterstitial);
        interstitialAd.setMonetizableTrackUrn(monetizableTrackUrn);
        return interstitialAd;
    }

    @AutoValue
    static abstract class ApiModel extends ApiBaseAdVisual {
        @JsonCreator
        public static ApiModel create(@JsonProperty("urn") Urn urn,
                                      @JsonProperty("image_url") String imageUrl,
                                      @JsonProperty("clickthrough_url") String clickthroughUrl,
                                      @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                                      @JsonProperty("tracking_click_urls") List<String> trackingClickUrls) {
            return new AutoValue_InterstitialAd_ApiModel(urn, imageUrl, clickthroughUrl, trackingImpressionUrls, trackingClickUrls);
        }
    }
}
