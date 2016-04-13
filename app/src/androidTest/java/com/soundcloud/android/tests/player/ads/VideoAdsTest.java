package com.soundcloud.android.tests.player.ads;

import android.net.Uri;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.screens.WhyAdsScreen;
import com.soundcloud.android.tests.TestConsts;

import org.hamcrest.Matchers;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.framework.matcher.player.IsSkipAllowed.SkipAllowed;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

@AdsTest
public class VideoAdsTest extends AdBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.LETTERBOX_VIDEO_PLAYLIST_URI;
    }

    public void testSkipIsNotAllowedOnAd() {
        swipeToAd();
        assertThat(playerElement, is(not(SkipAllowed())));

        playerElement.clickAdVideo();

        assertThat(playerElement, is(not(SkipAllowed())));
    }

    public void testTappingVideoTwiceResumesPlayingAd() {
        swipeToAd();
        playerElement.waitForPlayState();
        playerElement.clickAdVideo();
        assertThat(playerElement, is(not(playing())));
        playerElement.waitForPlayButton();
        playerElement.clickAdVideo();
        assertThat(playerElement, is(playing()));
    }

    public void testShowWhyAdsDialogWhenClickingWhyAds() {
        swipeToAd();
        WhyAdsScreen dialog = playerElement.clickWhyAdsForUpsell();
        assertThat(dialog, is(visible()));

        dialog.clickOK();
        assertThat(dialog, is(not(visible())));
    }

    public void testExpandsPlayerWhenAdStartsPlayingInCollapsedState() {
        playerElement
                .pressBackToCollapse()
                .waitForExpandedPlayer();
        playerElement.waitForAdPage();

        assertTrue(playerElement.isAdPageVisible());
        assertThat(playerElement, Matchers.is(expanded()));
    }

    public void testSwipeDownDoesntCollapsePlayer() {
        swipeToAd();
        solo.swipeDown();
        assertThat(playerElement.isCollapsed(), is(false));
    }

    public void testSkipAdShouldStartTheMonetizableTrack() {
        swipeToAd();
        playerElement
                .waitForAdToBeSkippable()
                .waitForSkipAdButton()
                .tapSkipAd();

        assertFalse(playerElement.isAdPageVisible());
    }
}
