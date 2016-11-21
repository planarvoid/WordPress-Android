package com.soundcloud.android.events;

import com.soundcloud.android.events.AdRequestEvent.AdsReceived;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AdRequestEventTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(30L);
    private static final Urn VIDEO_AD_URN = Urn.forAd("dfp", "video");
    private static final Urn AUDIO_AD_URN = Urn.forAd("dfp", "audio");
    private static final Urn INTERSTITIAL_AD_URN = Urn.forAd("dfp", "interstitial");
    private static final AdsReceived ALL_ADS_RECEIVED = AdsReceived.forPlayerAd(VIDEO_AD_URN,
                                                                                AUDIO_AD_URN,
                                                                                INTERSTITIAL_AD_URN);
    private static final String ADS_ENDPOINT = "endpoint/ads";

    @Test
    public void shouldCreateEventForAdRequestSuccess() {
        AdRequestEvent event = AdRequestEvent.adRequestSuccess("uuid",
                                                               Optional.of(TRACK_URN),
                                                               ADS_ENDPOINT,
                                                               ALL_ADS_RECEIVED,
                                                               false,
                                                               true);

        assertThat(event.getKind()).isEqualTo(AdRequestEvent.AD_REQUEST_SUCCESS_KIND);
        assertThat(event.get("monetizable_track_urn")).isEqualTo(TRACK_URN.toString());
        assertThat(event.get("request_endpoint")).isEqualTo(ADS_ENDPOINT);
        assertThat(event.adsReceived).isEqualTo(Optional.of(ALL_ADS_RECEIVED));
        assertThat(event.playerVisible).isFalse();
        assertThat(event.inForeground).isTrue();
    }

    @Test
    public void shouldCreateEventForAdRequestFailure() {
        AdRequestEvent event = AdRequestEvent.adRequestFailure("uuid",
                                                               Optional.of(TRACK_URN),
                                                               ADS_ENDPOINT,
                                                               false,
                                                               true);

        assertThat(event.getKind()).isEqualTo(AdRequestEvent.AD_REQUEST_FAILURE_KIND);
        assertThat(event.get("monetizable_track_urn")).isEqualTo(TRACK_URN.toString());
        assertThat(event.get("request_endpoint")).isEqualTo(ADS_ENDPOINT);
        assertThat(event.playerVisible).isFalse();
        assertThat(event.inForeground).isTrue();
    }

    @Test
    public void shouldCreateAdsReceivedWithAllAdType() {
        AdsReceived adsReceived = AdsReceived.forPlayerAd(VIDEO_AD_URN, AUDIO_AD_URN, INTERSTITIAL_AD_URN);

        assertAdData(adsReceived.ads, "video_ad", VIDEO_AD_URN);
        assertAdData(adsReceived.ads, "audio_ad", AUDIO_AD_URN);
        assertAdData(adsReceived.ads, "interstitial", INTERSTITIAL_AD_URN);
    }

    @Test
    public void shouldCreateAdsReceivedWithJustVideo() {
        AdsReceived adsReceived = AdsReceived.forPlayerAd(VIDEO_AD_URN, Urn.NOT_SET, Urn.NOT_SET);

        assertAdData(adsReceived.ads, "video_ad", VIDEO_AD_URN);
        assertThat(adsReceived.ads.containsKey("audio_ad")).isFalse();
        assertThat(adsReceived.ads.containsKey("interstitial")).isFalse();
    }

    @Test
    public void shouldCreateAdsReceivedWithJustAudio() {
        AdsReceived adsReceived = AdsReceived.forPlayerAd(Urn.NOT_SET, AUDIO_AD_URN, Urn.NOT_SET);

        assertThat(adsReceived.ads.containsKey("video_ad")).isFalse();
        assertAdData(adsReceived.ads, "audio_ad", AUDIO_AD_URN);
        assertThat(adsReceived.ads.containsKey("interstitial")).isFalse();
    }

    @Test
    public void shouldCreateAdsReceivedWithJustInterstitial() {
        AdsReceived adsReceived = AdsReceived.forPlayerAd(Urn.NOT_SET, Urn.NOT_SET, INTERSTITIAL_AD_URN);

        assertThat(adsReceived.ads.containsKey("video_ad")).isFalse();
        assertThat(adsReceived.ads.containsKey("audio_ad")).isFalse();
        assertAdData(adsReceived.ads, "interstitial", INTERSTITIAL_AD_URN);
    }

    @Test
    public void shouldCreateAdsReceivedWithAppInstallsAndVideoAds() {
        final List<Urn> appInstalls = Arrays.asList(Urn.forAd("1", "1"), Urn.forAd("1", "2"));
        final List<Urn> videos = Arrays.asList(Urn.forAd("2", "1"), Urn.forAd("2", "2"));
        AdsReceived adsReceived = AdsReceived.forStreamAds(appInstalls, videos);

        assertAdData(adsReceived.ads, "video_ad", videos);
        assertAdData(adsReceived.ads, "app_install", appInstalls);
        assertThat(adsReceived.ads.containsKey("audio_ad")).isFalse();
        assertThat(adsReceived.ads.containsKey("interstitial")).isFalse();
    }

    private void assertAdData(Map<String, Object> data, String key, Urn adUrn) {
        assertThat(data.containsKey(key)).isTrue();
        final Map<String, String> adData = (Map<String, String>) data.get(key);

        assertThat(adData.containsKey("urn")).isTrue();
        assertThat(adData.get("urn")).isEqualTo(adUrn.toString());
    }

    private void assertAdData(Map<String, Object> data, String key, List<Urn> urns) {
        assertThat(data.containsKey(key)).isTrue();
        final Map<String, List<String>> adData = (Map<String, List<String>>) data.get(key);

        assertThat(adData.containsKey("urns")).isTrue();
        for (Urn urn : urns) {
            assertThat(adData.get("urns")).contains(urn.toString());
        }
    }
}
