package com.soundcloud.android.tests.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrLogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class TrackingPlayerTest extends TrackingActivityTest<MainActivity> {
    private static final String TEST_SCENARIO = "Android-PlayerTest";

    private VisualPlayerElement playerElement;

    public TrackingPlayerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mrLoggaVerifier.startLogging();
        //mrLoggaVerifier.startRecording(TEST_SCENARIO);
    }

    public void ignore_testSwipingNextAndPreviousChangesTrack() {
        playTrackFromStream();
        String originalTrack = playerElement.getTrackTitle();

        playerElement.swipeNext();
        assertThat(originalTrack, is(not(equalTo(playerElement.getTrackTitle()))));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        mrLoggaVerifier.finishLogging();
        mrLoggaVerifier.isValid(TEST_SCENARIO);
        //mrLoggaVerifier.finishRecording();
    }

    private void playTrackFromStream() {
        playerElement = new StreamScreen(solo).clickFirstTrack();
        playerElement.waitForExpandedPlayer();
    }
}
