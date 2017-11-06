
package com.soundcloud.android.tests.player.ads;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile;
import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.player.IsSkipAllowed.SkipAllowed;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.screens.WhyAdsScreen;
import org.junit.Test;

import android.content.res.Resources;
import android.net.Uri;

@AdsTest
public class AudioAdTest extends AdBaseTest {

    private static final String SCENARIO_AUDIO_AD_QUARTILES = "specs/audio_ad_quartiles.spec";

    @Override
    protected Uri getUri() {
        return AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
    }

    @Override
    protected void addInitialStubMappings() {
        Resources resources = getInstrumentation().getContext().getResources();
        String body = readBodyOfFile(resources, "audio_ad_and_leave_behind.json");
        stubFor(get(urlPathMatching("/tracks/soundcloud%3Atracks%3A163824437/ads.*"))
                        .willReturn(aResponse().withStatus(200).withBody(body)));
    }


    // Override the default to remove the waits that are now unnecessary, since we are mocking the response.
    // Once all the ads tests are mocked, we can remove this and use this as the default.
    @Override
    protected void playAdPlaylist() {
        playAdPlaylistWithoutWaits();
    }

    @org.junit.Ignore
    @Test
    public void testQuartileEvents() throws Exception {
        mrLocalLocal.startEventTracking();

        swipeToAd();

        playerElement.waitForAudioAdToBeDone();
        playerElement.waitForLeaveBehindToLoad();
        assertThat("Should display leave behind", playerElement.isLeaveBehindVisible());

        mrLocalLocal.verify(SCENARIO_AUDIO_AD_QUARTILES);
    }

    @org.junit.Ignore
    @Test
    public void testSkipIsNotAllowedOnAd() throws Exception {
        swipeToAd();
        assertThat(playerElement, is(not(SkipAllowed())));

        if (playerElement.isFullbleedAd()) {
            playerElement.clickAdArtwork();
        }

        assertThat(playerElement, is(not(SkipAllowed())));
    }

    @org.junit.Ignore
    @Test
    public void testTappingFullBleedAdArtworkTwiceResumesPlayingAd() throws Exception {
        swipeToAd();
        if (playerElement.isFullbleedAd()) {
            playerElement.waitForPlayState();
            playerElement.clickAdArtwork();
            assertThat(playerElement, is(not(playing())));
            playerElement.waitForPlayButton();
            playerElement.clickAdArtwork();
            assertThat(playerElement, is(playing()));
        }
    }

    @Test
    public void testSkipShouldBeDisplayedWhenAdIsSkippable() throws Exception {
        swipeToAd();
        playerElement.waitForAdToBeSkippable();
        assertThat(playerElement, is(SkipAllowed()));
    }

    @org.junit.Ignore
    @Test
    public void testSkipAdShouldStartTheMonetizableTrack() throws Exception {
        swipeToAd();
        playerElement
                .waitForAdToBeSkippable()
                .waitForSkipAdButton()
                .tapSkipAd();

        assertFalse(playerElement.isAdPageVisible());
    }

    @org.junit.Ignore
    @Test
    public void testDoesNotOpenTrackWhileAdIsPlaying() throws Exception {
        swipeToAd();
        playerElement.pressBackToCollapse();
        waiter.waitForPlaybackToBePlaying();
        playlistDetailsScreen
                .scrollToBottom()
                .clickFirstTrack();

        assertThat(playerElement, is(collapsed()));
        assertThat(playerElement.isFooterAdTextVisible(), is(true));
    }

    @org.junit.Ignore
    @Test
    public void testShowWhyAdsDialogWhenClickingWhyAds() throws Exception {
        swipeToAd();
        WhyAdsScreen dialog = playerElement.clickWhyAdsForUpsell();
        assertThat(dialog, is(visible()));

        dialog.clickOK();
        assertThat(dialog, is(not(visible())));
    }

    @org.junit.Ignore
    @Test
    public void testCustomCTAButtonText() throws Exception {
        swipeToAd();

        assertEquals(playerElement.getAdCTAButtonText(), "TRY FREE FOR 30 DAYS");
    }

    @org.junit.Ignore
    @Test
    public void testExpandsPlayerWhenAdStartsPlayingInCollapsedState() throws Exception {
        playerElement
                .pressBackToCollapse()
                .waitForExpandedPlayer();
        playerElement.waitForAdPage();

        assertTrue(playerElement.isAdPageVisible());
        assertThat(playerElement, is(expanded()));
    }
}
