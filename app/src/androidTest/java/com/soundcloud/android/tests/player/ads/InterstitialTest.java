package com.soundcloud.android.tests.player.ads;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile;
import static com.soundcloud.android.tests.TestConsts.INTERSTITIAL_PLAYLIST_URI;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.annotation.AdsTest;
import org.junit.Test;

import android.content.res.Resources;
import android.net.Uri;

@AdsTest
public class InterstitialTest extends AdBaseTest {

    @Override
    protected Uri getUri() {
        return INTERSTITIAL_PLAYLIST_URI;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    // Override the default to remove the waits that are now unnecessary, since we are mocking the response.
    // Once all the ads tests are mocked, we can remove this and use this as the default.
    @Override
    protected void playAdPlaylist() {
        playAdPlaylistWithoutWaits();
    }

    @Override
    protected void addInitialStubMappings() {
        Resources resources = getInstrumentation().getContext().getResources();
        String body = readBodyOfFile(resources, "interstitial_ad.json");
        stubFor(get(urlPathMatching("/tracks/soundcloud%3Atracks%3A168135902/ads.*"))
                        .willReturn(aResponse().withStatus(200).withBody(body)));
    }

    @Test
    public void testShouldShowInterstitial() throws Exception {
        playerElement.swipeNext(); // to monetizableTrack
        assertTrue(playerElement.waitForPlayState());
        assertThat("Display interstitial", playerElement.waitForInterstitialToBeDisplayed());
    }
}
