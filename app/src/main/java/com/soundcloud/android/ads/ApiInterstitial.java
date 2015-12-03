package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;

import java.util.List;

class ApiInterstitial extends ApiBaseAdVisual {

    @JsonCreator
    public ApiInterstitial(
            @JsonProperty("urn") Urn urn,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("clickthrough_url") String clickthroughUrl,
            @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
            @JsonProperty("tracking_click_urls") List<String> trackingClickUrls) {
        super(urn, imageUrl, clickthroughUrl, trackingImpressionUrls, trackingClickUrls);
    }

}
