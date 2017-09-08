
package com.soundcloud.android.tests.player.ads;

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
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.screens.WhyAdsScreen;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

@Ignore // https://soundcloud.atlassian.net/browse/DROID-1754
@AdsTest
public class AudioAdTest extends AdBaseTest {

    public static final String SCENARIO_AUDIO_AD_QUARTILES = "specs/audio_ad_quartiles.spec";

    @Override
    protected Uri getUri() {
        return AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
    }

    @Test
    public void testQuartileEvents() throws Exception {
        mrLocalLocal.startEventTracking();

        swipeToAd();

        playerElement.waitForAudioAdToBeDone();
        mrLocalLocal.verify(SCENARIO_AUDIO_AD_QUARTILES);
    }

    @Test
    public void testSkipIsNotAllowedOnAd() throws Exception {
        swipeToAd();
        assertThat(playerElement, is(not(SkipAllowed())));

        if (playerElement.isFullbleedAd()) {
            playerElement.clickAdArtwork();
        }

        assertThat(playerElement, is(not(SkipAllowed())));
    }

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

    public void skip_testSkipShouldBeDisplayedWhenAdIsSkippable() {
        swipeToAd();
        playerElement.waitForAdToBeSkippable();
        assertThat(playerElement, is(SkipAllowed()));
    }

    @Test
    public void testSkipAdShouldStartTheMonetizableTrack() throws Exception {
        swipeToAd();
        playerElement
                .waitForAdToBeSkippable()
                .waitForSkipAdButton()
                .tapSkipAd();

        assertFalse(playerElement.isAdPageVisible());
    }

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

    @Test
    public void testShowWhyAdsDialogWhenClickingWhyAds() throws Exception {
        swipeToAd();
        WhyAdsScreen dialog = playerElement.clickWhyAdsForUpsell();
        assertThat(dialog, is(visible()));

        dialog.clickOK();
        assertThat(dialog, is(not(visible())));
    }

    @Test
    public void testCustomCTAButtonText() throws Exception {
        swipeToAd();

        assertEquals(playerElement.getAdCTAButtonText(), "TRY FREE FOR 30 DAYS");
    }

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
