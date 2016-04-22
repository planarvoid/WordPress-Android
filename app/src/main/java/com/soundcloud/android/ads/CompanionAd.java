package com.soundcloud.android.ads;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.net.Uri;

import java.util.List;

@AutoValue
public abstract class CompanionAd {

    public static CompanionAd create(ApiCompanionAd apiCompanionAd) {
        return new AutoValue_CompanionAd(
                apiCompanionAd.urn,
                Uri.parse(apiCompanionAd.imageUrl),
                extractClickThrough(apiCompanionAd),
                apiCompanionAd.trackingImpressionUrls,
                apiCompanionAd.trackingClickUrls,
                apiCompanionAd.ctaButtonText
        );
    }

    private static Optional<String> extractClickThrough(ApiCompanionAd apiCompanionAd) {
        return Strings.isBlank(apiCompanionAd.clickthroughUrl)
                ? Optional.<String>absent()
                : Optional.of(apiCompanionAd.clickthroughUrl);
    }

    public abstract Urn getAdUrn();

    public abstract Uri getImageUrl();

    public abstract Optional<String> getClickThroughUrl();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getClickUrls();

    public abstract Optional<String> getCallToActionButtonText();

}
