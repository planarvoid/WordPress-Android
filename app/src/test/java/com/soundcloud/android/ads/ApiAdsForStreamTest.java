package com.soundcloud.android.ads;

import com.soundcloud.android.testsupport.AndroidUnitTest;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiAdsForStreamTest extends AndroidUnitTest {
    private ApiVideoAd videoAd;
    private ApiAppInstallAd appInstallAd;

    @Before
    public void setUp() {
        videoAd = AdFixtures.getApiVideoAd();
        appInstallAd = AdFixtures.getApiAppInstall();
    }

    @Test
    public void getAppInstallsReturnsEmptyListOnEmptyAdsForStream() throws Exception {
        final ApiAdsForStream adsForStream = new ApiAdsForStream(Collections.<ApiAdWrapper>emptyList());
        assertThat(adsForStream.getAppInstalls()).isEmpty();
    }

    @Test
    public void getAppInstallsReturnsEmptyListWhenAdsForStreamHasOnlyVideoAd() throws Exception {
        final ApiAdsForStream adsForStream = new ApiAdsForStream(Arrays.asList(ApiAdWrapper.create(videoAd)));
        assertThat(adsForStream.getAppInstalls()).isEmpty();
    }

    @Test
    public void getAppInstallsReturnsAppInstallsFromAdsForStraem() throws Exception {
        final ApiAdsForStream adsForStream = AdFixtures.fullAdsForStream();
        assertThat(adsForStream.getAppInstalls()).contains(AppInstallAd.create(appInstallAd));
    }
}
