package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

import java.util.HashMap;
import java.util.List;

public class AdRequestEvent extends LegacyTrackingEvent {

    public static final String AD_REQUEST_SUCCESS_KIND = "AD_REQUEST_SUCCESS";
    public static final String AD_REQUEST_FAILURE_KIND = "AD_REQUEST_FAILURE";

    public final Optional<AdsReceived> adsReceived;
    public final boolean inForeground;
    public final boolean playerVisible;

    private AdRequestEvent(String kind, String uuid, Optional<Urn> monetizableUrn, String endpoint,
                           boolean playerVisible, boolean inForeground, Optional<AdsReceived> adsReceived) {
        super(kind, uuid);
        put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, monetizableUrn);
        put(PlayableTrackingKeys.KEY_ADS_ENDPOINT, endpoint);
        this.inForeground = inForeground;
        this.playerVisible = playerVisible;
        this.adsReceived = adsReceived;
    }

    public static AdRequestEvent adRequestSuccess(String uuid, Optional<Urn> monetizableUrn, String endpoint,
                                                  AdsReceived adsReceived, boolean playerVisible, boolean inForeground) {
        return new AdRequestEvent(AD_REQUEST_SUCCESS_KIND,
                                  uuid,
                                  monetizableUrn,
                                  endpoint,
                                  playerVisible,
                                  inForeground,
                                  Optional.of(adsReceived));
    }

    public static AdRequestEvent adRequestFailure(String uuid, Optional<Urn> monetizableUrn, String endpoint,
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
        private static final String APP_INSTALL_AD_KEY = "app_install";

        private static final String URN_KEY = "urn";
        private static final String URNS_KEY = "urns";

        private static final Function<Urn, String> URN_TO_STRING = new Function<Urn, String>() {
            public String apply(Urn input) {
                return input.toString();
            }
        };

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

        AdsReceived put(String key, Urn adUrn) {
            if (adUrn.isAd()) {
                final HashMap<String, String> adData = new HashMap<>(1);
                adData.put(URN_KEY, adUrn.toString());
                ads.put(key, adData);
            }
            return this;
        }

        AdsReceived put(String key, List<Urn> urns) {
            if (!urns.isEmpty()) {
                final HashMap<String, List<String>> adData = new HashMap<>(1);
                adData.put(URNS_KEY, Lists.transform(urns, URN_TO_STRING));
                ads.put(key, adData);
            }
            return this;
        }
    }
}
