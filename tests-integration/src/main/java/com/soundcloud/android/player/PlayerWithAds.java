package com.soundcloud.android.player;

import static com.soundcloud.android.tests.matcher.player.IsPlaying.Playing;
import static com.soundcloud.android.tests.matcher.player.IsSkipAllowed.SkipAllowed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.helpers.NavigationHelper;
import com.soundcloud.android.tests.helpers.PlayerHelper;

/*
 * We ignore all this test until we have a way
 * to get audio ads with the CI environnement.
 */
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

        final ExploreScreen screen = NavigationHelper.openExploreFromMenu(this);
        playerElement = PlayerHelper.openPlayer(this, screen);
        PlayerHelper.skipToAd(playerElement);    }

    public void ignoreTestSkipIsNotAllowedOnAd() throws Exception {
        assertThat(playerElement, is(not(SkipAllowed())));
        playerElement.clickArtwork();
        assertThat(playerElement, is(not(SkipAllowed())));
    }

    public void ignoreTestTappingArtworkPausesAd() {
        playerElement.clickArtwork();
        assertThat(playerElement, is(not(Playing())));
    }

    public void ignoreTestTappingArtworkTwiceResumePlayingAd() {
        playerElement.clickArtwork();
        playerElement.clickArtwork();
        assertThat(playerElement, is(Playing()));
    }
}
