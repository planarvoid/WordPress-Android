package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.collections.PropertySet;

import android.net.Uri;

import java.util.List;

class ApiInterstitial extends ApiBaseAdVisual implements PropertySetSource {

    @JsonCreator
    public ApiInterstitial(
            @JsonProperty("urn") String urn,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("clickthrough_url") String clickthroughUrl,
            @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
            @JsonProperty("tracking_click_urls") List<String> trackingClickUrls) {
        super(urn, imageUrl, clickthroughUrl, trackingImpressionUrls, trackingClickUrls);
    }

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                InterstitialProperty.INTERSTITIAL_URN.bind(urn),
                InterstitialProperty.IMAGE_URL.bind(imageUrl),
                InterstitialProperty.CLICK_THROUGH_URL.bind(Uri.parse(clickthroughUrl)),
                InterstitialProperty.TRACKING_IMPRESSION_URLS.bind(trackingImpressionUrls),
                InterstitialProperty.TRACKING_CLICK_URLS.bind(trackingClickUrls));
    }
}
