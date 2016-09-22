package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.HashMap;

public class AdRequestEvent extends TrackingEvent {

    public static final String AD_REQUEST_SUCCESS_KIND = "AD_REQUEST_SUCCESS";
    public static final String AD_REQUEST_FAILURE_KIND = "AD_REQUEST_FAILURE";

    public final Optional<AdsReceived> adsReceived;
    public final boolean inForeground;
    public final boolean playerVisible;

    private AdRequestEvent(String kind, String uuid, Urn monetizableUrn, String endpoint,
                           boolean playerVisible, boolean inForeground, Optional<AdsReceived> adsReceived) {
        super(kind, uuid);
        put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, monetizableUrn.toString());
        put(PlayableTrackingKeys.KEY_ADS_ENDPOINT, endpoint);
        this.inForeground = inForeground;
        this.playerVisible = playerVisible;
        this.adsReceived = adsReceived;
    }

    public static AdRequestEvent adRequestSuccess(String uuid, Urn monetizableUrn, String endpoint,
                                                  AdsReceived adsReceived, boolean playerVisible, boolean inForeground) {
        return new AdRequestEvent(AD_REQUEST_SUCCESS_KIND,
                                  uuid,
                                  monetizableUrn,
                                  endpoint,
                                  playerVisible,
                                  inForeground,
                                  Optional.of(adsReceived));
    }

    public static AdRequestEvent adRequestFailure(String uuid, Urn monetizableUrn, String endpoint,
                                                  boolean playerVisible, boolean inForeground) {
        return new AdRequestEvent(AD_REQUEST_FAILURE_KIND,
                                  uuid,
                                  monetizableUrn,
                                  endpoint,
                                  playerVisible,
                                  inForeground,
                                  Optional.<AdsReceived>absent());
    }

    public static class AdsReceived {
        private static final String VIDEO_AD_KEY = "video_ad";
        private static final String AUDIO_AD_KEY = "audio_ad";
        private static final String INTERSTITIAL_AD_KEY = "interstitial";
        private static final String URN_KEY = "urn";

        public final HashMap<String, Object> ads;

        public AdsReceived(Urn videoAdUrn, Urn audioAdUrn, Urn interstitialAdUrn) {
            ads = new HashMap<>(3);
            put(VIDEO_AD_KEY, videoAdUrn);
            put(AUDIO_AD_KEY, audioAdUrn);
            put(INTERSTITIAL_AD_KEY, interstitialAdUrn);
        }

        private void put(String key, Urn adUrn) {
            if (adUrn.isAd()) {
                final HashMap<String, String> adData = new HashMap<>(1);
                adData.put(URN_KEY, adUrn.toString());
                ads.put(key, adData);
            }
        }
    }
}
