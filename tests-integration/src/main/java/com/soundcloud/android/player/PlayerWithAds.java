package com.soundcloud.android.player;

import static com.soundcloud.android.tests.matcher.player.IsCollapsed.Collapsed;
import static com.soundcloud.android.tests.matcher.player.IsExpanded.Expanded;
import static com.soundcloud.android.tests.matcher.player.IsPlaying.Playing;
import static com.soundcloud.android.tests.matcher.player.IsSkipAllowed.SkipAllowed;
import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.WhyAdsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.with.With;

import java.util.concurrent.TimeUnit;

public class PlayerWithAds extends ActivityTestCase<MainActivity> {

    private static final int TRACK_BEFORE_AD_LENGTH_MILISECONDS = (int) TimeUnit.SECONDS.toMillis(10);

    private VisualPlayerElement playerElement;
    private PlaylistDetailsScreen playlistDetailsScreen;

    public PlayerWithAds() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.adUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        playMonetizablePlaylist();
    }

    public void testSkipIsNotAllowedOnAd() {
        swipeToAd();
        assertThat(playerElement, is(not(SkipAllowed())));
        playerElement.clickArtwork();
        playerElement.tapTrackPageNext();
        assertThat(playerElement, is(not(SkipAllowed())));
    }

    public void testTappingArtworkPausesAd() {
        swipeToAd();
        playerElement.clickArtwork();
        assertThat(playerElement, is(not(Playing())));
    }

    public void testTappingArtworkTwiceResumePlayingAd() {
        swipeToAd();
        playerElement.clickArtwork();
        playerElement.clickArtwork();
        assertThat(playerElement, is(Playing()));
    }

    public void testSkipShouldBeDisplayedWhenAdIsSkippable() {
        swipeToAd();
        assertThat(playerElement, is(not(SkipAllowed())));
        playerElement.waitForAdToBeSkippable();
        playerElement.waitForSkipAdButton();
        assertThat(playerElement, is(SkipAllowed()));
    }

    public void testSkipAdShouldStartTheMonetizableTrack() {
        swipeToAd();
        playerElement.waitForAdToBeSkippable();
        playerElement.waitForSkipAdButton();
        String adTrackTitle = playerElement.getTrackTitle();
        playerElement.tapSkipAd();
        assertThat(adTrackTitle, is(not(equalTo(playerElement.getTrackTitle()))));
    }

    public void testDoesNotOpenTrackWhileAdIsPlaying() {
        swipeToAd();
        playerElement.clickArtwork();
        playerElement.pressBackToCollapse();
        String footerTrackCreator = playerElement.getFooterTrackCreator();
        playlistDetailsScreen.scrollToBottom();
        playlistDetailsScreen.clickFirstTrack();

        assertThat(playerElement, is(Collapsed()));
        assertThat(playerElement.getFooterTrackCreator(), equalTo(footerTrackCreator));
    }

    public void testShowWhyAdsDialogWhenClickingWhyAds() {
        swipeToAd();
        WhyAdsScreen dialog = playerElement.clickWhyAds();
        assertThat(dialog, is(Visible()));

        dialog.clickOK();
        assertThat(dialog, is(not(Visible())));
    }

    public void testExpandsPlayerWhenAdStartsPlayingInCollapsedState() {
        playerElement.pressBackToCollapse();
        playerElement.waitForTrackToFinish(TRACK_BEFORE_AD_LENGTH_MILISECONDS);
        playerElement.waitForAdPage();

        assertTrue(playerElement.isAdPageVisible());
        assertThat(playerElement, is(Expanded()));
    }

    /**
     *
     * We have 2 tracks before the monetizable track, due to a known behaviour (#2025),
     * in which you will not get an ad on a fresh install if you open play queue and the monetizable track is the second track
     * This will happen when we stop using the policies endpoint to get track policies on a play queue change
     *
     */
    private void playMonetizablePlaylist() {
        playlistDetailsScreen = menuScreen.open().clickPlaylist().clickPlaylist(With.text("Monetizable Playlist"));
        playerElement = playlistDetailsScreen.clickFirstTrack();
        playerElement.waitForExpandedPlayer();
        playerElement.swipeNext();
        playerElement.waitForAdToBeFetched();
    }

    private void swipeToAd() {
        playerElement.swipeNext();
    }
}
