package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class TrackingPlayerTest extends TrackingActivityTest<MainActivity> {
    private static final String TEST_SCENARIO = "player-test";

    public TrackingPlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    public void diabled_testPlayAndPauseTrackFromStream() {
        mrLoggaVerifier.startLogging();

        final VisualPlayerElement playerElement =
                menuScreen
                        .open()
                        .clickStream()
                        .clickTrack(1);

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));

        finishMrLogga();
    }

    private void finishMrLogga() {
        mrLoggaVerifier.stopLogging();
        mrLoggaVerifier.assertScenario(TEST_SCENARIO);
    }

}
