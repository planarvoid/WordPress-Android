package com.soundcloud.android.ads;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class ApiAdsForStreamTest extends AndroidUnitTest {
    private static final Date CURRENT_DATE = new Date();
    private ApiVideoAd videoAd;
    private TestDateProvider dateProvider = new TestDateProvider(CURRENT_DATE);

    @Before
    public void setUp() {
        videoAd = AdFixtures.getApiVideoAd();
    }

    @Test
    public void getAppInstallsReturnsEmptyListOnEmptyAdsForStream() throws Exception {
        final ApiAdsForStream adsForStream = new ApiAdsForStream(Collections.<ApiAdWrapper>emptyList());
        assertThat(adsForStream.getAppInstalls(dateProvider)).isEmpty();
    }

    @Test
    public void getAppInstallsReturnsEmptyListWhenAdsForStreamHasOnlyVideoAd() throws Exception {
        final ApiAdsForStream adsForStream = new ApiAdsForStream(Arrays.asList(ApiAdWrapper.create(videoAd)));
        assertThat(adsForStream.getAppInstalls(dateProvider)).isEmpty();
    }

    @Test
    public void getAppInstallsReturnsAppInstallsFromAdsForStream() throws Exception {
        final ApiAppInstallAd apiAppInstall = AdFixtures.getApiAppInstall();
        final ApiAdsForStream adsForStream = new ApiAdsForStream(newArrayList(
                ApiAdWrapper.create(apiAppInstall),
                ApiAdWrapper.create(AdFixtures.getApiVideoAd())
        ));


        final List<AppInstallAd> appInstalls = adsForStream.getAppInstalls(dateProvider);
        assertThat(appInstalls.size()).isEqualTo(1);
        assertThat(appInstalls.get(0).getAdUrn()).isEqualTo(apiAppInstall.getAdUrn());
    }
}
