package com.soundcloud.android.ads;

import com.soundcloud.android.testsupport.AndroidUnitTest;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class ApiAdsForStreamTest extends AndroidUnitTest {
    private ApiVideoAd videoAd;

    @Before
    public void setUp() {
        videoAd = AdFixtures.getApiVideoAd();
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
    public void getAppInstallsReturnsAppInstallsFromAdsForStream() throws Exception {
        final ApiAppInstallAd apiAppInstall = AdFixtures.getApiAppInstall();
        final ApiAdsForStream adsForStream = new ApiAdsForStream(newArrayList(
                ApiAdWrapper.create(apiAppInstall),
                ApiAdWrapper.create(AdFixtures.getApiVideoAd())
        ));


        final List<AppInstallAd> appInstalls = adsForStream.getAppInstalls();
        assertThat(appInstalls.size()).isEqualTo(1);
        assertThat(appInstalls.get(0).getAdUrn()).isEqualTo(apiAppInstall.getAdUrn());
    }
}
