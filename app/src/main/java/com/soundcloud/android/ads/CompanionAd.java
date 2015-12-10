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
                apiCompanionAd.ctaButtonText,
                apiCompanionAd.displayProperties.defaultTextColor,
                apiCompanionAd.displayProperties.defaultBackgroundColor,
                apiCompanionAd.displayProperties.pressedTextColor,
                apiCompanionAd.displayProperties.pressedBackgroundColor,
                apiCompanionAd.displayProperties.focusedTextColor,
                apiCompanionAd.displayProperties.focusedBackgroundColor
        );
    }

    public abstract Urn getAdUrn();

    public abstract Uri getImageUrl();

    public abstract Uri getClickThroughUrl();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getClickUrls();

    public abstract Optional<String> getCallToActionButtonText();

    public abstract String getDefaultTextColor();

    public abstract String getDefaultBackgroundColor();

    public abstract String getPressedTextColor();

    public abstract String getPressedBackgroundColor();

    public abstract String getFocusedTextColor();

    public abstract String getFocusedBackgroundColor();
}
