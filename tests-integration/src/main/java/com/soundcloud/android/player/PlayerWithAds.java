package com.soundcloud.android.player;

import static com.soundcloud.android.tests.matcher.player.IsCollapsed.Collapsed;
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

public class PlayerWithAds extends ActivityTestCase<MainActivity> {

    private VisualPlayerElement playerElement;
    private PlaylistDetailsScreen playlistDetailsScreen;

    public PlayerWithAds() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.adUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        playlistDetailsScreen = menuScreen.open().clickPlaylist().clickPlaylist(With.text("Monetizable Playlist"));
        playerElement = playlistDetailsScreen.clickFirstTrack();
        playerElement.waitForExpandedPlayer();
        playerElement.swipeNext();
    }

    public void testSkipIsNotAllowedOnAd() {
        assertThat(playerElement, is(not(SkipAllowed())));
        playerElement.clickArtwork();
        assertThat(playerElement, is(not(SkipAllowed())));
    }

    public void testTappingArtworkPausesAd() {
        playerElement.clickArtwork();
        assertThat(playerElement, is(not(Playing())));
    }

    public void testTappingArtworkTwiceResumePlayingAd() {
        playerElement.clickArtwork();
        playerElement.clickArtwork();
        assertThat(playerElement, is(Playing()));
    }

    public void testSkipShouldBeDisplayedAfter15sec() {
        assertThat(playerElement, is(not(SkipAllowed())));
        playerElement.waitForSkipAdButton();
        assertThat(playerElement, is(SkipAllowed()));
    }

    public void testSkipAdShouldStartTheMonetizableTrack() {
        playerElement.waitForSkipAdButton();
        String adTrackTitle = playerElement.getTrackTitle();
        playerElement.tapSkipAd();
        assertThat(adTrackTitle, is(not(equalTo(playerElement.getTrackTitle()))));
    }

    public void testDoesNotOpenTrackWhileAdIsPlaying() {
        playerElement.clickArtwork();
        playerElement.pressBackToCollapse();
        String footerTrackCreator = playerElement.getFooterTrackCreator();
        playlistDetailsScreen.scrollToBottom();
        playlistDetailsScreen.clickFirstTrack();

        assertThat(playerElement, is(Collapsed()));
        assertThat(playerElement.getFooterTrackCreator(), equalTo(footerTrackCreator));
    }

    public void testShowWhyAdsDialogWhenClickingWhyAds() {
        WhyAdsScreen dialog = playerElement.clickWhyAds();
        assertThat(dialog, is(Visible()));

        dialog.clickOK();
        assertThat(dialog, is(not(Visible())));
    }
}
