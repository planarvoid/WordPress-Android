package com.soundcloud.android.ads;

import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

abstract class ApiBaseAdVisual {

    public final String urn;
    public final String imageUrl;
    public final String clickthroughUrl;
    public final List<String> trackingImpressionUrls;
    public final List<String> trackingClickUrls;

    public ApiBaseAdVisual(String urn, String imageUrl, String clickthroughUrl, List<String> trackingImpressionUrls,
                           List<String> trackingClickUrls) {
        this.urn = urn;
        this.imageUrl = imageUrl;
        this.clickthroughUrl = clickthroughUrl;
        this.trackingImpressionUrls = trackingImpressionUrls;
        this.trackingClickUrls = trackingClickUrls;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
                .add("urn", urn)
                .add("imageUrl", imageUrl)
                .add("clickthroughUrl", clickthroughUrl)
                .add("trackingImpressionUrls", trackingImpressionUrls)
                .add("trackingClickUrls", trackingClickUrls)
                .toString();
    }

}
