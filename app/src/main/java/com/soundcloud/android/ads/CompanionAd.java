package com.soundcloud.android.ads;

import android.net.Uri;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class CompanionAd {

    public static CompanionAd create(ApiCompanionAd apiCompanionAd) {
        return new AutoValue_CompanionAd(
                apiCompanionAd.urn,
                Uri.parse(apiCompanionAd.imageUrl),
                Uri.parse(apiCompanionAd.clickthroughUrl),
                apiCompanionAd.trackingImpressionUrls,
                apiCompanionAd.trackingClickUrls,
                apiCompanionAd.ctaButtonText
        );
    }

    public abstract Urn getAdUrn();

    public abstract Uri getImageUrl();

    public abstract Uri getClickThroughUrl();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getClickUrls();

    public abstract Optional<String> getCallToActionButtonText();

}
