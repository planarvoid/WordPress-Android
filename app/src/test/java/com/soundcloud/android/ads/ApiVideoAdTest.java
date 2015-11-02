package com.soundcloud.android.ads;

import com.soundcloud.android.testsupport.AndroidUnitTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiVideoAdTest extends AndroidUnitTest {

    private ApiVideoAd videoAd;

    @Before
    public void setUp() {
        videoAd = AdFixtures.getApiVideoAd();
    }

    @Test
    public void getVideoSourcesReturnsNonEmptyListOfSources() throws Exception {
        assertThat(videoAd.getVideoSources()).isNotEmpty();
    }
}

