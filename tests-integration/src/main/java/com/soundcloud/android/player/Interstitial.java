package com.soundcloud.android.player;

import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.with.With;

public class Interstitial extends ActivityTestCase<MainActivity> {

    private VisualPlayerElement playerElement;
    private PlaylistDetailsScreen playlistDetailsScreen;

    public Interstitial() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.adUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();
        setSkipOnReleaseBuild();

        playInterstitialPlaylist();
    }

    public void testFinishAdShouldShowInterstitial() {
        playerElement.swipeNext(); // to monetizableTrack
        playerElement.waitForAdOverlayToLoad();
        assertThat(playerElement.interstitial(), is(visible()));
    }

    private void playInterstitialPlaylist() {
        playlistDetailsScreen = menuScreen.open().clickPlaylist().clickPlaylist(With.text("Interstitial Playlist"));
        playerElement = playlistDetailsScreen.clickFirstTrack();
        playerElement.waitForExpandedPlayer();
        playerElement.swipeNext();
        playerElement.waitForAdToBeFetched();
    }
}
