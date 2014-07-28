package com.soundcloud.android.player;

import static com.soundcloud.android.tests.matcher.player.IsPlaying.Playing;
import static com.soundcloud.android.tests.matcher.player.IsSkipAllowed.SkipAllowed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.helpers.NavigationHelper;
import com.soundcloud.android.tests.helpers.PlayerHelper;

public class PlayerWithAds extends ActivityTestCase<MainActivity> {

    private VisualPlayerElement playerElement;

    public PlayerWithAds() {
        super(MainActivity.class);
        setDependsOn(Feature.VISUAL_PLAYER);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        final StreamScreen streamScreen = new StreamScreen(solo);
        final ExploreScreen screen = NavigationHelper.openExploreFromMenu(streamScreen);
        playerElement = PlayerHelper.openPlayer(this, screen);
        PlayerHelper.skipToAd(playerElement);
    }

    public void testSkipIsNotAllowedOnAd() throws Exception {
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

    public void ignoreSkipShouldBeDisplayedAfter15sec() throws Exception {
        assertThat(playerElement, is(not(SkipAllowed())));
        playerElement.waitForSkipAdButton();
        assertThat(playerElement, is(SkipAllowed()));
    }

    public void ignoreSkipAdShouldStartTheMonetizableTrack() throws Exception {
        playerElement.waitForSkipAdButton();
        String adTrackTitle = playerElement.getTrackTitle();
        playerElement.tapSkipAd();
        assertThat(adTrackTitle, is(not(equalTo(playerElement.getTrackTitle()))));
    }
}
