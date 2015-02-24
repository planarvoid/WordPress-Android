package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ApiAdsForTrackTest {

    @Test
    public void hasInterstitialIsFalseWithEmptyAdsForTrack() throws Exception {
        final ApiAdsForTrack apiAdsForTrack = new ApiAdsForTrack(Arrays.<ApiAdWrapper>asList());
        expect(apiAdsForTrack.hasInterstitialAd()).toBeFalse();
    }

    @Test
    public void hasInterstitialIsFalseWithNoInterstitial() throws Exception {
        final ApiAdsForTrack apiAdsForTrack = new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(ModelFixtures.create(ApiAudioAd.class))));
        expect(apiAdsForTrack.hasInterstitialAd()).toBeFalse();
    }

    @Test
    public void hasInterstitialIsTrueWithInterstitial() throws Exception {
        final ApiAdsForTrack apiAdsForTrack = new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(ModelFixtures.create(ApiAudioAd.class), ModelFixtures.create(ApiInterstitial.class))));
        expect(apiAdsForTrack.hasInterstitialAd()).toBeTrue();
    }

    @Test
    public void hasAudioAdIsFalseWithEmptyAdsForTrack() throws Exception {
        final ApiAdsForTrack apiAdsForTrack = new ApiAdsForTrack(Arrays.<ApiAdWrapper>asList());
        expect(apiAdsForTrack.hasAudioAd()).toBeFalse();
    }

    @Test
    public void hasAudioAdIsFalseWithNoAudioAd() throws Exception {
        final ApiAdsForTrack apiAdsForTrack = new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(ModelFixtures.create(ApiInterstitial.class))));
        expect(apiAdsForTrack.hasAudioAd()).toBeFalse();
    }

    @Test
    public void hasAudioAdIsTrueWithAudioAd() throws Exception {
        final ApiAdsForTrack apiAdsForTrack = new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(ModelFixtures.create(ApiAudioAd.class), ModelFixtures.create(ApiInterstitial.class))));
        expect(apiAdsForTrack.hasAudioAd()).toBeTrue();
    }

    @Test
    public void getInterstitialReturnsNullWithEmptyAdsForTrack() throws Exception {
        final ApiAdsForTrack actual = new ApiAdsForTrack(Arrays.<ApiAdWrapper>asList());
        expect(actual.interstitialAd()).toBeNull();
    }

    @Test
    public void getInterstitialReturnsNullWithNoInterstitial() throws Exception {
        final ApiAdsForTrack actual = new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(ModelFixtures.create(ApiAudioAd.class))));
        expect(actual.interstitialAd()).toBeNull();
    }

    @Test
    public void getInterstitialReturnsInterstitial() throws Exception {
        final ApiInterstitial interstitial = ModelFixtures.create(ApiInterstitial.class);
        final ApiAdsForTrack apiAdsForTrack = new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(ModelFixtures.create(ApiAudioAd.class), interstitial)));
        expect(apiAdsForTrack.interstitialAd()).toBe(interstitial);
    }

    @Test
    public void getAudioAdIsReturnNullWithEmptyAdsForTrack() throws Exception {
        final ApiAdsForTrack actual = new ApiAdsForTrack(Arrays.<ApiAdWrapper>asList());
        expect(actual.audioAd()).toBeNull();
    }

    @Test
    public void getAudioAdReturnsNullWithNoAudioAd() throws Exception {
        final ApiAdsForTrack actual = new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(ModelFixtures.create(ApiInterstitial.class))));
        expect(actual.audioAd()).toBeNull();
    }

    @Test
    public void getAudioAdReturnsAudioAd() throws Exception {
        final ApiAudioAd apiAudioAd = ModelFixtures.create(ApiAudioAd.class);
        final ApiAdsForTrack apiAdsForTrack = new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(apiAudioAd, ModelFixtures.create(ApiInterstitial.class))));
        expect(apiAdsForTrack.audioAd()).toBe(apiAudioAd);
    }
}