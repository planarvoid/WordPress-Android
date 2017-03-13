package com.soundcloud.android.ads;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiAdsForTrackTest extends AndroidUnitTest {
    private ApiAudioAd audioAd;
    private ApiVideoAd videoAd;
    private ApiInterstitial interstitial;

    @Before
    public void setUp() {
        audioAd = AdFixtures.getApiAudioAd();
        videoAd = AdFixtures.getApiVideoAd();
        interstitial = AdFixtures.getApiInterstitial();
    }

    @Test
    public void getInterstitialReturnsAbsentOnEmptyAdsForTrack() throws Exception {
        final ApiAdsForTrack adsForTrack = new ApiAdsForTrack(Arrays.asList());
        assertThat(adsForTrack.interstitialAd()).isEqualTo(Optional.<ApiInterstitial>absent());
    }

    @Test
    public void getInterstitialReturnsAbsentWhenNoInterstitialAdsForTrack() throws Exception {
        final ApiAdsForTrack adsForTrack = AdFixtures.audioAdsForTrack();
        assertThat(adsForTrack.interstitialAd()).isEqualTo(Optional.<ApiInterstitial>absent());
    }

    @Test
    public void getInterstitialReturnsAdOnInterstitialAdsForTrack() throws Exception {
        final ApiAdsForTrack adsForTrack = new ApiAdsForTrack(Collections.singletonList(ApiAdWrapper.create(interstitial)));
        assertThat(adsForTrack.interstitialAd()).isEqualTo(Optional.of(interstitial));
    }

    @Test
    public void getAudioReturnsAbsentOnEmptyAdsForTrack() throws Exception {
        final ApiAdsForTrack adsForTrack = new ApiAdsForTrack(Arrays.asList());
        assertThat(adsForTrack.audioAd()).isEqualTo(Optional.<ApiAudioAd>absent());
    }

    @Test
    public void getAudioReturnsAbsentWhenNoAudioAdsForTrack() throws Exception {
        final ApiAdsForTrack adsForTrack = AdFixtures.interstitialAdsForTrack();
        assertThat(adsForTrack.audioAd()).isEqualTo(Optional.<ApiAudioAd>absent());
    }

    @Test
    public void getAudioAdReturnsAdOnAudioAdsForTrack() throws Exception {
        final ApiAdsForTrack adsForTrack = new ApiAdsForTrack(Collections.singletonList(ApiAdWrapper.create(audioAd)));
        assertThat(adsForTrack.audioAd()).isEqualTo(Optional.of(audioAd));
    }

    @Test
    public void getVideoReturnsAbsentOnEmptyAdsForTrack() throws Exception {
        final ApiAdsForTrack adsForTrack = new ApiAdsForTrack(Arrays.asList());
        assertThat(adsForTrack.videoAd()).isEqualTo(Optional.<ApiVideoAd>absent());
    }

    @Test
    public void getVideoReturnsAbsentWhenNoVideoAdsForTrack() throws Exception {
        final ApiAdsForTrack adsForTrack = AdFixtures.audioAdsForTrack();
        assertThat(adsForTrack.videoAd()).isEqualTo(Optional.<ApiVideoAd>absent());
    }

    @Test
    public void getVideoAdReturnsAdOnVideoAdsForTrack() throws Exception {
        final ApiAdsForTrack adsForTrack = new ApiAdsForTrack(Collections.singletonList(ApiAdWrapper.create(videoAd)));
        assertThat(adsForTrack.videoAd()).isEqualTo(Optional.of(videoAd));
    }
}
