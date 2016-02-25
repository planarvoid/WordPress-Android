package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;

import java.util.HashMap;

public class AdDeliveryEvent extends TrackingEvent {

    public static final String AD_DELIVERED_KIND = "AD_DELIVERED";
    public static final String AD_FAILED_KIND = "AD_FAILED";

    public final boolean adsRequested;
    public final boolean inForeground;
    public final boolean playerVisible;

    public AdsReceived adsReceived;
    public boolean adOptimized;
    public Urn adUrn;

    private AdDeliveryEvent(String kind, Urn monetizableUrn, boolean adsRequested, String endpoint, boolean playerVisible, boolean inForeground) {
        super(kind, System.currentTimeMillis());
        this.put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, monetizableUrn.toString());
        this.put(AdTrackingKeys.KEY_ADS_ENDPOINT, endpoint);
        this.adsRequested = adsRequested;
        this.inForeground = inForeground;
        this.playerVisible = playerVisible;
    }

    public static AdDeliveryEvent adDelivered(Urn monetizableUrn, Urn adUrn, String endpoint, AdsReceived adsReceived,
                                              boolean adOptimized, boolean playerVisible, boolean inForeground) {
        AdDeliveryEvent event = new AdDeliveryEvent(AD_DELIVERED_KIND, monetizableUrn, true, endpoint, playerVisible, inForeground);
        event.adOptimized = adOptimized;
        event.adUrn = adUrn;
        event.adsReceived = adsReceived;
        return event;
    }

    public static AdDeliveryEvent adsRequestFailed(Urn monetizableUrn, String endpoint, boolean playerVisible, boolean inForeground) {
        return new AdDeliveryEvent(AD_FAILED_KIND, monetizableUrn, true, endpoint, playerVisible, inForeground);
    }

    public static class AdsReceived {
        private static final String VIDEO_AD_KEY = "video_ad";
        private static final String AUDIO_AD_KEY = "audio_ad";
        private static final String INTERSTITIAL_AD_KEY = "interstitial";
        private static final String URN_KEY = "urn";

        public final HashMap<String, Object> ads;

        public AdsReceived(Urn videoAdUrn, Urn audioAdUrn, Urn interstitialAdUrn) {
            this.ads = new HashMap<>();
            this.put(VIDEO_AD_KEY, videoAdUrn);
            this.put(AUDIO_AD_KEY, audioAdUrn);
            this.put(INTERSTITIAL_AD_KEY, interstitialAdUrn);
        }

        private void put(String key, Urn adUrn) {
            if (adUrn.isAd()) {
                final HashMap<String, String> adData = new HashMap<>();
                adData.put(URN_KEY, adUrn.toString());
                this.ads.put(key, adData);
            }
        }
    }
}
