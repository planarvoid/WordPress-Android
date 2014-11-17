package com.soundcloud.android.player;

import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.R;
import com.soundcloud.android.tests.TestUser;
import com.soundcloud.android.tests.with.With;

public class LeaveBehindTest extends ActivityTestCase<MainActivity> {

    private VisualPlayerElement playerElement;
    private PlaylistDetailsScreen playlistDetailsScreen;

    public LeaveBehindTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.adUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        setRunBasedOnResource(R.bool.run_ads_tests);

        playMonetizablePlaylist();
    }

    public void testFinishAdShouldShowLeaveBehind() {
        swipeToAd();
        playerElement.waitForAdToBeDone();
        playerElement.waitForAdOverlayToLoad();
        assertThat(playerElement.leaveBehind(), is(visible()));
    }

    /**
     *
     * We have 2 tracks before the monetizable track, due to a known behaviour (#2025),
     * in which you will not get an ad on a fresh install if you open play queue and the monetizable track is the second track
     * This will happen when we stop using the policies endpoint to get track policies on a play queue change
     *
     */
    private void playMonetizablePlaylist() {
        playlistDetailsScreen = menuScreen.open().clickPlaylist().clickPlaylist(With.text("[auto] AudioAd and LeaveBehind Playlist"));
        playerElement = playlistDetailsScreen.clickFirstTrack();
        playerElement.waitForExpandedPlayer();
        playerElement.swipeNext();
        playerElement.waitForAdToBeFetched();
    }

    private void swipeToAd() {
        playerElement.swipeNext();
        playerElement.waitForAdPage();
    }
}
