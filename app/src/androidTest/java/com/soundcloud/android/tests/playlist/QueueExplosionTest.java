package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

@com.soundcloud.android.framework.annotation.QueueExplosionTest
public class QueueExplosionTest extends ActivityTest<MainActivity> {

    public QueueExplosionTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playlistExplosionUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.EXPLODE_PLAYLISTS_IN_PLAYQUEUES);
        super.setUp();
    }

    public void testExplodesStreamPlaylist() {
        final VisualPlayerElement visualPlayerElement =
                mainNavHelper.goToStream().clickFirstNotPromotedTrackCard();

        visualPlayerElement.waitForTheExpandedPlayerToPlayNextTrack();

        assertThat(visualPlayerElement.getTrackTitle(), is(equalTo("Sounds from Friday afternoon")));
    }

    public void testExplodesPostedPlaylist() {
        final ProfileScreen profileScreen =
                mainNavHelper.goToYou().clickMyProfileLink();

        final VisualPlayerElement visualPlayerElement = profileScreen.playTrack(0);
        visualPlayerElement.waitForTheExpandedPlayerToPlayNextTrack();

        assertThat(visualPlayerElement.getTrackTitle(), is(equalTo("Sounds from Friday afternoon")));
    }

    public void testExplodesLikedPlaylist() {
        final ProfileScreen profileScreen = mainNavHelper
                .goToYou()
                .clickMyProfileLink()
                .touchLikesTab();

        final VisualPlayerElement visualPlayerElement = profileScreen.playTrack(0);
        visualPlayerElement.waitForTheExpandedPlayerToPlayNextTrack();

        assertThat(visualPlayerElement.getTrackTitle(), is(equalTo("Sounds from Friday afternoon")));
    }
}
