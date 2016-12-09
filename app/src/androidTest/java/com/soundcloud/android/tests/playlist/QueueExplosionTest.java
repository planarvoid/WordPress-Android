package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

import java.util.concurrent.TimeUnit;

public class QueueExplosionTest extends ActivityTest<MainActivity> {

    private static final int TEN_SECONDS = (int) TimeUnit.SECONDS.toMillis(10);

    public QueueExplosionTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.playlistExplosionUser;
    }

    public void testExplodesStreamPlaylist() {
        final VisualPlayerElement visualPlayerElement =
                mainNavHelper.goToStream().clickFirstNotPromotedTrackCard();

        visualPlayerElement.waitForTheExpandedPlayerToPlayNextTrack(TEN_SECONDS);

        assertThat(visualPlayerElement.getTrackTitle(), is(equalTo("Sounds from Friday afternoon")));
    }
}
