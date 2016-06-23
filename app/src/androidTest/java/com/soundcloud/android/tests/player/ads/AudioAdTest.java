package com.soundcloud.android.tests.player.ads;

import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.player.IsSkipAllowed.SkipAllowed;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.screens.WhyAdsScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

@AdsTest
public class AudioAdTest extends AdBaseTest {

    public static final String SCENARIO_AUDIO_AD_QUARTILES = "audio_ad_quartiles";

    @Override
    protected Uri getUri() {
        return TestConsts.AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
    }

    @EventTrackingTest
    public void testQuartileEvents() {
        startEventTracking();

        swipeToAd();

        playerElement.waitForAudioAdToBeDone();
        finishEventTracking(SCENARIO_AUDIO_AD_QUARTILES);
    }

    public void testSkipIsNotAllowedOnAd() {
        swipeToAd();
        assertThat(playerElement, is(not(SkipAllowed())));

        if (playerElement.isFullbleedAd()) {
            playerElement.clickAdArtwork();
        }

        assertThat(playerElement, is(not(SkipAllowed())));
    }

    public void testTappingFullBleedAdArtworkTwiceResumesPlayingAd() {
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

    public void skip_testSkipShouldBeDisplayedWhenAdIsSkippable() {
        swipeToAd();
        playerElement.waitForAdToBeSkippable();
        assertThat(playerElement, is(SkipAllowed()));
    }

    public void testSkipAdShouldStartTheMonetizableTrack() {
        swipeToAd();
        playerElement
                .waitForAdToBeSkippable()
                .waitForSkipAdButton()
                .tapSkipAd();

        assertFalse(playerElement.isAdPageVisible());
    }

    public void testDoesNotOpenTrackWhileAdIsPlaying() {
        swipeToAd();
        playerElement.pressBackToCollapse();
        waiter.waitForPlaybackToBePlaying();
        playlistDetailsScreen
                .scrollToBottom()
                .clickFirstTrack();

        assertThat(playerElement, is(collapsed()));
        assertThat(playerElement.isFooterAdTextVisible(), is(true));
    }

    public void testShowWhyAdsDialogWhenClickingWhyAds() {
        swipeToAd();
        WhyAdsScreen dialog = playerElement.clickWhyAdsForUpsell();
        assertThat(dialog, is(visible()));

        dialog.clickOK();
        assertThat(dialog, is(not(visible())));
    }

    public void testCustomCTAButtonText() {
        swipeToAd();

        assertEquals(playerElement.getAdCTAButtonText(), "TRY FREE FOR 30 DAYS");
    }

    public void testExpandsPlayerWhenAdStartsPlayingInCollapsedState() {
        playerElement
                .pressBackToCollapse()
                .waitForExpandedPlayer();
        playerElement.waitForAdPage();

        assertTrue(playerElement.isAdPageVisible());
        assertThat(playerElement, is(expanded()));
    }
}
