package com.soundcloud.android.ads;

import android.net.Uri;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class LeaveBehindAd extends OverlayAdData {

    public static LeaveBehindAd create(ApiLeaveBehind apiLeaveBehind, Urn audioAdUrn) {
        return new AutoValue_LeaveBehindAd(
                apiLeaveBehind.urn,
                MonetizationType.LEAVE_BEHIND,
                apiLeaveBehind.imageUrl,
                Uri.parse(apiLeaveBehind.clickthroughUrl),
                apiLeaveBehind.trackingImpressionUrls,
                apiLeaveBehind.trackingClickUrls,
                audioAdUrn
        );
    }

    public abstract Urn getAudioAdUrn();
}
