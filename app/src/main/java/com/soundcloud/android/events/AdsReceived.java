package com.soundcloud.android.events;

import com.soundcloud.android.model.ContentStringHelper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;

import java.util.HashMap;
import java.util.List;

public class AdsReceived {
    private static final String VIDEO_AD_KEY = "video_ad";
    private static final String AUDIO_AD_KEY = "audio_ad";
    private static final String INTERSTITIAL_AD_KEY = "interstitial";
    private static final String APP_INSTALL_AD_KEY = "app_install";
    private static final String DISPLAY_AD_KEY = "display";

    private static final String URN_KEY = "urn";
    private static final String URNS_KEY = "urns";

    public final HashMap<String, Object> ads;

    private AdsReceived() {
        ads = new HashMap<>(3);
    }

    public static AdsReceived forPlayerAd(Urn videoAdUrn, Urn audioAdUrn, Urn interstitialAdUrn) {
        return new AdsReceived().put(VIDEO_AD_KEY, videoAdUrn)
                                .put(AUDIO_AD_KEY, audioAdUrn)
                                .put(INTERSTITIAL_AD_KEY, interstitialAdUrn);
    }

    public static AdsReceived forStreamAds(List<Urn> appInstalls, List<Urn> videoAds) {
        return new AdsReceived().put(APP_INSTALL_AD_KEY, appInstalls)
                                .put(VIDEO_AD_KEY, videoAds);
    }

    public static AdsReceived forPrestitalAds(Urn displayUrn) {
        return new AdsReceived().put(DISPLAY_AD_KEY, displayUrn);
    }

    private AdsReceived put(String key, Urn adUrn) {
        if (adUrn.isAd()) {
            final HashMap<String, String> adData = new HashMap<>(1);
            adData.put(URN_KEY, adUrn.toString());
            ads.put(key, adData);
        }
        return this;
    }

    private AdsReceived put(String key, List<Urn> urns) {
        if (!urns.isEmpty()) {
            final HashMap<String, List<String>> adData = new HashMap<>(1);
            adData.put(URNS_KEY, Lists.transform(urns, ContentStringHelper::toString));
            ads.put(key, adData);
        }
        return this;
    }
}
