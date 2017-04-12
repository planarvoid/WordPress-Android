package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;

import java.util.List;

abstract class ApiBaseAdVisual {
    public abstract Urn adUrn();
    public abstract String imageUrl();
    public abstract String clickthroughUrl();
    public abstract List<String> trackingImpressionUrls();
    public abstract List<String> trackingClickUrls();
}
