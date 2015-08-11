package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@EventTrackingTest
public class LikesEventGatewayAudioTest extends TrackingActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_LIKES = "audio-events-v1-main-likes";

    public LikesEventGatewayAudioTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.EVENTLOGGER_AUDIO_V1);
        super.setUp();
    }

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testPlayAndPauseTrackFromLikes() {
        final TrackLikesScreen trackLikesScreen = menuScreen
                .open()
                .clickLikes();

        startEventTracking();

        final VisualPlayerElement playerElement =
                trackLikesScreen.clickTrack(0);

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));

        finishEventTracking(TEST_SCENARIO_LIKES);
    }
}
