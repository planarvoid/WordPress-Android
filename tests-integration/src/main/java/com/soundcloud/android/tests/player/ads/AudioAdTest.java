package com.soundcloud.android.tests.player.ads;

import static com.soundcloud.android.framework.matcher.player.IsCollapsed.collapsed;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.Playing;
import static com.soundcloud.android.framework.matcher.player.IsSkipAllowed.SkipAllowed;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.screens.WhyAdsScreen;

public class AudioAdTest extends AdBaseTest {

    public void testSkipIsNotAllowedOnAd() {
        playMonetizablePlaylist();

        swipeToAd();
        assertThat(playerElement, is(not(SkipAllowed())));
        playerElement.clickArtwork();
        assertThat(playerElement, is(not(SkipAllowed())));
    }

    public void testTappingArtworkPausesAd() {
        playMonetizablePlaylist();

        swipeToAd();
        playerElement.clickArtwork();
        assertThat(playerElement, is(not(Playing())));
    }

    public void testTappingArtworkTwiceResumePlayingAd() {
        playMonetizablePlaylist();

        swipeToAd();
        playerElement.clickArtwork();
        playerElement.waitForPlayButton();
        playerElement.clickArtwork();
        assertThat(playerElement, is(Playing()));
    }



    public void skip_testSkipShouldBeDisplayedWhenAdIsSkippable() {
        playMonetizablePlaylist();

        swipeToAd();
        playerElement.waitForAdToBeSkippable();
        assertThat(playerElement, is(SkipAllowed()));
    }

    public void testSkipAdShouldStartTheMonetizableTrack() {
        playMonetizablePlaylist();

        swipeToAd();
        playerElement.waitForAdToBeSkippable();
        playerElement.waitForSkipAdButton();
        playerElement.tapSkipAd();

        assertFalse(playerElement.isAdPageVisible());
    }

    public void testDoesNotOpenTrackWhileAdIsPlaying() {
        playMonetizablePlaylist();

        swipeToAd();
        playerElement.clickArtwork();
        playerElement.pressBackToCollapse();
        String footerTrackCreator = playerElement.getFooterTrackCreator();
        playlistDetailsScreen.scrollToBottom();
        playlistDetailsScreen.clickFirstTrack();

        assertThat(playerElement, is(collapsed()));
        assertThat(playerElement.getFooterTrackCreator(), equalTo(footerTrackCreator));
    }

    public void testShowWhyAdsDialogWhenClickingWhyAds() {
        playMonetizablePlaylist();

        swipeToAd();
        WhyAdsScreen dialog = playerElement.clickWhyAds();
        assertThat(dialog, is(visible()));

        dialog.clickOK();
        assertThat(dialog, is(not(visible())));
    }

    public void testExpandsPlayerWhenAdStartsPlayingInCollapsedState() {
        playMonetizablePlaylist();

        playerElement.pressBackToCollapse();
        playerElement.waitForExpandedPlayer();
        playerElement.waitForAdPage();

        assertTrue(playerElement.isAdPageVisible());
        assertThat(playerElement, is(expanded()));
    }
}
