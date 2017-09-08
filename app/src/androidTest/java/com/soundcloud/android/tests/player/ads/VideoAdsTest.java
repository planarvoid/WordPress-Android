package com.soundcloud.android.tests.player.ads;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.player.IsSkipAllowed.SkipAllowed;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.LETTERBOX_VIDEO_PLAYLIST_URI;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.screens.WhyAdsScreen;
import com.soundcloud.android.tests.TestConsts;
import org.hamcrest.Matchers;
import org.junit.Test;

import android.net.Uri;

@AdsTest
public class VideoAdsTest extends AdBaseTest {

    public static final String SCENARIO_VIDEO_AD_QUARTILES = "specs/video_ad_quartiles.spec";

    @Override
    protected Uri getUri() {
        return LETTERBOX_VIDEO_PLAYLIST_URI;
    }

    @Test
    public void testQuartileEvents() throws Exception {
        mrLocalLocal.startEventTracking();

        swipeToAd();

        playerElement.waitForVideoAdToBeDone();
        mrLocalLocal.verify(SCENARIO_VIDEO_AD_QUARTILES);
    }

    @Test
    public void testSkipIsNotAllowedOnAd() throws Exception {
        swipeToAd();
        assertThat(playerElement, is(not(SkipAllowed())));

        playerElement.clickAdVideo();

        assertThat(playerElement, is(not(SkipAllowed())));
    }

    @Test
    public void testTappingVideoTwiceResumesPlayingAd() throws Exception {
        swipeToAd();
        playerElement.waitForPlayState();
        playerElement.clickAdVideo();
        assertThat(playerElement, is(not(playing())));
        playerElement.waitForPlayButton();
        playerElement.clickAdVideo();
        assertThat(playerElement, is(playing()));
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
    public void testExpandsPlayerWhenAdStartsPlayingInCollapsedState() throws Exception {
        playerElement
                .pressBackToCollapse()
                .waitForExpandedPlayer();
        playerElement.waitForAdPage();

        assertTrue(playerElement.isAdPageVisible());
        assertThat(playerElement, Matchers.is(expanded()));
    }

    @Test
    public void testSwipeDownDoesntCollapsePlayer() throws Exception {
        swipeToAd();
        solo.swipeDown();
        assertThat(playerElement.isCollapsed(), is(false));
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
}
