package com.soundcloud.android.ads;

import android.net.Uri;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class InterstitialAd extends OverlayAdData {

    private static InterstitialAd create(ApiInterstitial apiInterstitial) {
        return new AutoValue_InterstitialAd(
                apiInterstitial.urn,
                MonetizationType.INTERSTITIAL,
                apiInterstitial.imageUrl,
                Uri.parse(apiInterstitial.clickthroughUrl),
                apiInterstitial.trackingImpressionUrls,
                apiInterstitial.trackingClickUrls
        );
    }

    public static InterstitialAd create(ApiInterstitial apiInterstitial, Urn monetizableTrackUrn) {
        final InterstitialAd interstitialAd = create(apiInterstitial);
        interstitialAd.setMonetizableTrackUrn(monetizableTrackUrn);
        return interstitialAd;
    }
}
