package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import android.net.Uri;

import java.util.List;

@AutoValue
public abstract class VisualPrestitialAd extends VisualAdData {

    public static VisualPrestitialAd create(ApiModel apiModel) {
        return new AutoValue_VisualPrestitialAd(apiModel.adUrn(),
                                                MonetizationType.PRESTITIAL,
                                                apiModel.imageUrl(),
                                                Uri.parse(apiModel.clickthroughUrl()),
                                                apiModel.trackingImpressionUrls(),
                                                apiModel.trackingClickUrls());
    }

    @AutoValue
    static abstract class ApiModel extends ApiBaseAdVisual {
        @JsonCreator
        public static ApiModel create(@JsonProperty("urn") Urn urn,
                                      @JsonProperty("image_url") String imageUrl,
                                      @JsonProperty("clickthrough_url") String clickthroughUrl,
                                      @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                                      @JsonProperty("tracking_click_urls") List<String> trackingClickUrls) {
            return new AutoValue_VisualPrestitialAd_ApiModel(urn, imageUrl, clickthroughUrl, trackingImpressionUrls, trackingClickUrls);
        }
    }
}
