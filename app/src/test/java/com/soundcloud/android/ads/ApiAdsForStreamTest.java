package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ApiAdsForStreamTest {
    private static final Date CURRENT_DATE = new Date();
    private VideoAd.ApiModel videoAd;
    private TestDateProvider dateProvider = new TestDateProvider(CURRENT_DATE);

    @Before
    public void setUp() {
        videoAd = AdFixtures.getApiVideoAd();
    }

    @Test
    public void getAppInstallsReturnsEmptyListOnEmptyAdsForStream() throws Exception {
        final ApiAdsForStream adsForStream = new ApiAdsForStream(Collections.emptyList());
        assertThat(adsForStream.getAds(dateProvider)).isEmpty();
    }

    @Test
    public void getAdsReturnsAdsFromAdsForStream() throws Exception {
        final AppInstallAd.ApiModel apiAppInstall = AdFixtures.getApiAppInstall();
        final ApiAdsForStream adsForStream = new ApiAdsForStream(newArrayList(
                ApiAdWrapper.create(apiAppInstall),
                ApiAdWrapper.create(AdFixtures.getApiVideoAd())
        ));


        final List<AdData> appInstalls = adsForStream.getAds(dateProvider);
        assertThat(appInstalls.size()).isEqualTo(2);
        assertThat(appInstalls.get(0).adUrn()).isEqualTo(apiAppInstall.adUrn());
        assertThat(appInstalls.get(1).adUrn()).isEqualTo(videoAd.adUrn());
    }
}
