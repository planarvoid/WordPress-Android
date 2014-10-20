package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;

import java.util.List;

public class ApiInterstitial extends ApiVisualAd implements PropertySetSource {

    private final String urn;

    @JsonCreator
    public ApiInterstitial(
            @JsonProperty("urn") String urn,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("clickthrough_url") String clickthroughUrl,
            @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
            @JsonProperty("tracking_click_urls") List<String> trackingClickUrls) {
        super(imageUrl, clickthroughUrl, trackingImpressionUrls, trackingClickUrls);
        this.urn = urn;
    }

    public String getUrn() {
        return urn;
    }

    @Override
    public String toString() {
        return "ApiInterstitial{" +
                "urn='" + urn + '\'' +
                '}';
    }

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                InterstitialProperty.INTERSTITIAL_URN.bind(urn),
                InterstitialProperty.IMAGE_URL.bind(getImageUrl()),
                InterstitialProperty.CLICK_THROUGH_URL.bind(Uri.parse(getClickthroughUrl())),
                InterstitialProperty.TRACKING_IMPRESSION_URLS.bind(getTrackingImpressionUrls()),
                InterstitialProperty.TRACKING_CLICK_URLS.bind(getTrackingClickUrls()));
    }
}
