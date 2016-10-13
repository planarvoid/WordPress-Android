package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class QueueExplosionTest extends ActivityTest<MainActivity> {

    public QueueExplosionTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playlistExplosionUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testExplodesStreamPlaylist() {
        final VisualPlayerElement visualPlayerElement =
                mainNavHelper.goToStream().clickFirstNotPromotedTrackCard();

        visualPlayerElement.waitForTheExpandedPlayerToPlayNextTrack();

        assertThat(visualPlayerElement.getTrackTitle(), is(equalTo("Sounds from Friday afternoon")));
    }
}
